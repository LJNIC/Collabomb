using import glm
using import String
using import struct 
using import enum
using import Array
using import UTF-8
using import .level-load
import .bottle
from bottle.graphics let Sprite

global directions = (arrayof ivec2 (ivec2 1 0) (ivec2 0 1) (ivec2 -1 0) (ivec2 0 -1))
global scaling = 3.0

struct GameSnapshot
    tiles  : (Array TileType)
    player : ivec2
    bombs  : (Array Bomb)
    boxes  : (Array ivec2)

    inline __typecall (cls board-state)
        local this = (super-type.__typecall cls)

        # as long as we keep the names consistent with the 
          ones used in BoardState, this will work ;)
        va-map
            inline (element)
                for ?? in (getattr board-state element)
                    'append (getattr this element) (copy ??)
            _ 'tiles 'bombs 'boxes

        this.player = (copy board-state.player)
        this

fn rollback-state (history board)
    if ((countof history) > 0)
        let snapshot = ('last history)
        board.player = (copy snapshot.player)

        va-map
            inline (element)
                let dst = (getattr board element)
                let src = (getattr snapshot element)
                
                'clear dst
                for ?? in src
                    'append dst (copy ??)
            _ 'tiles 'bombs 'boxes

        'pop history
        true
    else
        false

global current-level : u32 1
global level-count : u32 5
global board : BoardState
global history : (Array GameSnapshot)

@@ 'on bottle.config
fn (cfg)
    cfg

fn create-quad (x y)
    let normal-x = (x / 160)
    let normal-y = (y / 160)
    (vec4 normal-x normal-y (normal-x + (16 / 160)) (normal-y + (16 / 160)))

global player-spr  : vec4
global wall-spr    : vec4
global goal-spr    : vec4 
global back-spr    : vec4
global fragile-spr : vec4
global box-spr     : vec4
global tileset  : Sprite
global bomb-quads : (Array vec4)

fn load-level-num (num)
    local level-file = (.. "levels/level" (String (dec num)) ".txt")
    (load-level level-file)

@@ 'on bottle.load
fn ()
    board = (load-level-num current-level)
    player-spr = (create-quad 0 0)
    goal-spr = (create-quad 16 0)
    wall-spr = (create-quad 32 0)
    fragile-spr = (create-quad 48 0)
    box-spr = (create-quad 64 0)    
    back-spr = (create-quad 128 0)
    tileset = (Sprite "tileset.png")
    for i in (range 10)
        'append bomb-quads (create-quad (i * 16) 16)
    'append bomb-quads (create-quad (9 * 16) 16)

fn try-move (delta)
    # we record the state before trying to move, but only append
    # if succesful in doing so.
    let snapshot = (GameSnapshot board)

    new-pos := board.player + delta
    let proj = ('tile@ board new-pos)
    inline free? (t)
        or
            t == TileType.Free
            t == TileType.Goal

    for bomb in board.bombs
        if (new-pos == bomb.pos)
            let bproj = ('tile@ board (bomb.pos + delta))
            if (free? bproj)
                bomb.pos += delta
            else
                return false

    for box in board.boxes
        if (new-pos == box)
            let bproj = ('tile@ board (box + delta))
            if (free? bproj)
                box += delta
            else
                return false

    if (free? proj)
        board.player = new-pos

    'append history snapshot
    true

fn win-condition? ()
    # check if we solved the level
    for box in board.boxes
        if (('tile@ board box) != TileType.Goal)
            return false
    true

fn explode-bomb (index)
    let bomb = (board.bombs @ index)
    if (bomb.timer == 0)
        for direction in directions
            let pos = (bomb.pos + direction)
            if (('tile@ board pos) == TileType.Fragile)
                'clear@ board pos
            
            for i in (rrange (countof board.boxes))
                let box = (board.boxes @ i)
                if (box == pos)
                    'remove board.boxes i
            
            for other-bomb in board.bombs
                if (other-bomb.pos == pos)
                    other-bomb.timer = 1
                    
        'remove board.bombs index

@@ 'on bottle.update
fn (dt)
    inline pressed? (key)
        let held? rep = (bottle.input.holding? key 0.0)
        rep

    let moved? =
        if (pressed? 'Left)
            try-move (ivec2 -1 0)
        elseif (pressed? 'Right)
            try-move (ivec2 1 0)
        elseif (pressed? 'Down)
            try-move (ivec2 0 -1)
        elseif (pressed? 'Up)
            try-move (ivec2 0 1)
        else
            false

    if moved?
        for i in (rrange (countof board.bombs))
            let bomb = (board.bombs @ i)
            if (bomb.timer < 10)
                bomb.timer -= 1
        # we have to do this separately so they don't all explode at once if there is a chain reaction
        for i in (rrange (countof board.bombs))
            explode-bomb i

    if (bottle.input.pressed? 'B)
        board = (load-level-num current-level)

    # undo
    # this is a bit confusing. Initially I added this variable so I would know movement
      happened also with an undo (for sound effects), but for game logic you only care about movement forwards in time.
    local moved? = moved?
    if (bottle.input.pressed? 'A)
        moved? = (rollback-state history board)
    
    if (or (win-condition?) (bottle.input.pressed? 'Y))
        current-level += 1
        if (current-level <= level-count)
            board = (load-level-num current-level)
        'clear history

    if (and (bottle.input.pressed? 'X) (current-level > 1))
        current-level -= 1
        board = (load-level-num current-level)
        'clear history

@@ 'on bottle.draw
fn ()
    using import itertools
    for x y in (dim (unpack board.dimensions))
        let t = ('tile@ board (ivec2 x y))
        let tsprite =
            switch t
            case TileType.Free
                if (board.player == (ivec2 x y))
                    player-spr
                else
                    back-spr
            case TileType.Wall
                wall-spr
            case TileType.Goal
                goal-spr
            case TileType.Fragile
                fragile-spr
            default
                back-spr
        
        bottle.graphics.sprite tileset ((vec2 x y) * (16 * scaling)) (quad = tsprite) (scale = (vec2 scaling))

    for bomb in board.bombs
        bottle.graphics.sprite tileset ((vec2 bomb.pos) * (16 * scaling)) (quad = (bomb-quads @ (bomb.timer - 1))) (scale = (vec2 scaling))

    for box in board.boxes
        bottle.graphics.sprite tileset ((vec2 box) * (16 * scaling)) (quad = box-spr) (scale = (vec2 scaling))

bottle.run;


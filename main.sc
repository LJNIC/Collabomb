using import glm
using import String
using import struct 
using import enum
using import Array
using import UTF-8
import .bottle
from bottle.graphics let Sprite

global directions = (arrayof ivec2 (ivec2 1 0) (ivec2 0 1) (ivec2 -1 0) (ivec2 0 -1))
global scaling = 3.0

global levels =
    arrayof String
        """"XXXXXXXXXX
            XOOOOOOOOX
            XGOOOVOOPX
            XOOOOOOOOX
            XXXXXXXXXX

        """"XXXXXXXXXXXX
            XOOFOOOOOOOX
            XGFOO3OPOVOX
            XOOFOOOOOOOX
            XXXXXXXXXXXX

        """"XXXXXXXXXXXXX
            XOOOOOOOOOOOX
            XOOFOOOOOOOOX
            XOFGFOX9OPOOX
            XOOFOOOOOOOOX
            XXXXXXXXXXXXX

        """"XXXXXXXXXXXXX
            XOOOOXGXOOOOX
            XOOOOXVXOOOOX
            XOXXXXFXXXXOX
            XOXOOOOOOOXOX
            XOXOOO9OOOXOX
            XOFOO8P7OOFOX
            XVXOOOOOOOXVX
            XGXOOOOOOOXGX
            XXXXXXXXXXXXX

enum TileType plain
    Free
    Wall
    Fragile
    Goal

struct Bomb plain
    pos   : ivec2
    timer : u32

struct BoardState
    tiles      : (Array TileType)
    dimensions : ivec2
    bombs      : (Array Bomb)
    boxes      : (Array ivec2)
    player     : ivec2

    inline tile@ (self pos)
        self.tiles @ (pos.y * self.dimensions.x + pos.x)
    
    inline clear@ (self pos)
        self.tiles @ (pos.y * self.dimensions.x + pos.x) = TileType.Free

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

fn parse-board (n)
    let board-str = (levels @ n)

    # we assume the first line dictates width for the whole board.
    local board : BoardState
    fold (width = 0) for c in board-str
        if (c == 10:i8)
            board.dimensions.x = width
            break width
        width + 1

    fold (x y = 0 0) for c in board-str
        switch c
        case (char "X")
            'append board.tiles TileType.Wall
        case (char "O")
            'append board.tiles TileType.Free
        case (char "G")
            'append board.tiles TileType.Goal
        case (char "P")
            'append board.tiles TileType.Free
            board.player = (ivec2 x y)
        case (char "F")
            'append board.tiles TileType.Fragile
        case (char "V")
            'append board.tiles TileType.Free
            'append board.boxes (ivec2 x y) 
        pass (char "1") 
        pass (char "2")
        pass (char "3")
        pass (char "4")
        pass (char "5")
        pass (char "6")
        pass (char "7")
        pass (char "8")
        pass (char "9")
        do
            'append board.tiles TileType.Free
            local bomb : Bomb 
            bomb.pos = (ivec2 x y)
            bomb.timer = c - 48
            'append board.bombs bomb
        case 10:i8
            board.dimensions.y += 1
            repeat 0 (y + 1)
        default
            assert false "unrecognized tile type"
            unreachable;

        _ (x + 1) y
    deref board

global current-level : u32 0
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

@@ 'on bottle.load
fn ()
    board = (parse-board current-level)
    player-spr = (create-quad 0 0)
    goal-spr = (create-quad 16 0)
    wall-spr = (create-quad 32 0)
    fragile-spr = (create-quad 48 0)
    box-spr = (create-quad 64 0)    
    back-spr = (create-quad 128 0)
    tileset = (Sprite "tileset.png")
    for i in (range 9)
        'append bomb-quads (create-quad (i * 16) 16)

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

@@ 'on bottle.update
fn (dt)
    let moved? =
        if (bottle.input.pressed? 'Left)
            try-move (ivec2 -1 0)
        elseif (bottle.input.pressed? 'Right)
            try-move (ivec2 1 0)
        elseif (bottle.input.pressed? 'Down)
            try-move (ivec2 0 -1)
        elseif (bottle.input.pressed? 'Up)
            try-move (ivec2 0 1)
        elseif (bottle.input.pressed? 'B)
            true
        else
            false

    if moved?
        for i in (rrange (countof board.bombs))
            let bomb = (board.bombs @ i)
            bomb.timer -= 1
            if (bomb.timer == 0)
                for direction in directions
                    let pos = (bomb.pos + direction)
                    if (('tile@ board pos) == TileType.Fragile)
                        'clear@ board pos
                'remove board.bombs i

    # undo
    # this is a bit confusing. Initially I added this variable so I would know movement
      happened also with an undo (for sound effects), but for game logic you only care about movement forwards in time.
    local moved? = moved?
    if (bottle.input.pressed? 'A)
        moved? = (rollback-state history board)
    
    if (win-condition?)
        current-level += 1
        if (current-level < (countof levels))
            board = (parse-board current-level)
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


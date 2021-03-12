using import glm
using import String
using import struct 
using import enum
using import Array
using import UTF-8
import .bottle
from bottle.graphics let Sprite

global directions = (arrayof ivec2 (ivec2 1 0) (ivec2 0 1) (ivec2 -1 0) (ivec2 0 -1))

global levels =
    arrayof String
        """"XXXXXXXXXX
            XOOFOOOOOX
            XGFOOBOOPX
            XOOFOOOOOX
            XXXXXXXXXX

        """"XXXXXXXXXXXXX
            XGXOOOOOOOOOX
            XOOOOOOOOOOOX
            XOXOOOXXXXXXX
            XOOOOOOOBOOPX
            XXXOOOXXXXXXX
            XXXXXXXXXXXXX

        """"XXXXXXXXXXXXX
            XOOOOOGOOOOOX
            XOOOOOBOOOOOX
            XGOOOBPBOOOGX
            XOOOOOBOOOOOX
            XOOOOOGOOOOOX
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
    player     : ivec2

    inline tile@ (self pos)
        self.tiles @ (pos.y * self.dimensions.x + pos.x)
    
    inline clear@ (self pos)
        self.tiles @ (pos.y * self.dimensions.x + pos.x) = TileType.Free

struct GameSnapshot
    player : ivec2
    bombs  : (Array Bomb)

    inline __typecall (cls board-state)
        local bombs : (Array Bomb)
        for bomb in board-state.bombs
            'append bombs (copy bomb)

        super-type.__typecall cls
            player = (copy board-state.player)
            bombs = (deref bombs)

fn rollback-state (history board)
    if ((countof history) > 0)
        let snapshot = ('last history)
        board.player = (copy snapshot.player)
        for i bomb in (enumerate snapshot.bombs)
            board.bombs @ i = (copy bomb)
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
        case (char "B")
            'append board.tiles TileType.Free
            local bomb : Bomb 
            bomb.pos = (ivec2 x y)
            bomb.timer = 5
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

global player-spr : Sprite
global wall-spr : Sprite
global goal-spr : Sprite
global bomb-spr : Sprite
global back-spr : Sprite

@@ 'on bottle.load
fn ()
    board = (parse-board current-level)
    wall-spr = (Sprite "grey.png")
    goal-spr = (Sprite "blue.png")
    bomb-spr = (Sprite "brown.png")
    back-spr = (Sprite "black.png")
    player-spr = (Sprite "square.png")

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
    if (free? proj)
        board.player = new-pos

    'append history snapshot
    true

fn win-condition? ()
    # check if we solved the level
    if (('tile@ board board.player) == TileType.Goal)
        true
    else
        false

@@ 'on bottle.update
fn ()
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

    # undo
    local moved? = moved?
    if (bottle.input.pressed? 'A)
        moved? = (rollback-state history board)
    
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
                wall-spr
            default
                back-spr

        bottle.graphics.sprite tsprite ((vec2 x y) * 16)

    for bomb in board.bombs
        bottle.graphics.sprite bomb-spr ((vec2 bomb.pos) * 16)

bottle.run;


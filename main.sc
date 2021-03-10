using import glm
using import String
using import struct 
using import enum
using import Array
using import UTF-8
import .bottle
from bottle.graphics let Sprite

global levels =
    arrayof String
        """"XXXXXXXXXX
            XGOOBOOOPX
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
    Goal

struct BoardState
    tiles      : (Array TileType)
    dimensions : ivec2
    boxes      : (Array ivec2)
    player     : ivec2

    inline tile@ (self pos)
        self.tiles @ (pos.y * self.dimensions.x + pos.x)

struct GameSnapshot
    player : ivec2
    boxes : (Array ivec2)

    inline __typecall (cls board-state)
        local boxes : (Array ivec2)
        for box in board-state.boxes
            'append boxes (copy box)

        super-type.__typecall cls
            player = (copy board-state.player)
            boxes = (deref boxes)

fn rollback-state (history board)
    if ((countof history) > 0)
        let snapshot = ('last history)
        board.player = (copy snapshot.player)
        for i box in (enumerate snapshot.boxes)
            board.boxes @ i = (copy box)
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
        case (char "B")
            'append board.tiles TileType.Free
            'append board.boxes (ivec2 x y)
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
global box-spr : Sprite
global back-spr : Sprite

@@ 'on bottle.load
fn ()
    board = (parse-board current-level)
    player-spr = (Sprite "square.png")
    wall-spr = (Sprite "grey.png")
    goal-spr = (Sprite "blue.png")
    box-spr = (Sprite "square.png")
    back-spr = (Sprite "brown.png")


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
        else
            false

    # undo
    local moved? = moved?
    if (bottle.input.pressed? 'A)
        moved? = (rollback-state history board)
    
    if moved?
        print "moved"
    else 
        print "didn't move"

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
            default
                back-spr

        bottle.graphics.sprite tsprite ((vec2 x y) * 16)

    for box in board.boxes
        bottle.graphics.sprite box-spr ((vec2 box) * 16)

bottle.run;


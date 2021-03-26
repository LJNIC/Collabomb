import .bottle
using import glm
using import String
using import UTF-8
using import struct 
using import enum
using import Array

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

fn parse-csv (input)
    using import Array
    using import String
    using import UTF-8

    local values : (Array String)
    local value : String
    # not UTF-8 aware.
    loop (idx = 0)
        if (idx == (countof input))
            'append values (copy value)
            break;
        c := input @ idx

        if (c == (char ","))
            'append values (copy value)
            'clear value
        else
            'append value c
            ;
        idx + 1
    values

# ground tile layer
#    - = floor (indicates free passage)
#    # = wall (blocked passage)
#    . = goal
# object layer
#    ~ = destructible wall (can be blown up)
#    b = box
#    B(n) = bomb, where n is an arbitrary long number of ticks remaining for explosion

fn string->num (str)
    let strtol = (extern 'strtol (function u64 (mutable@ i8) (mutable@ (mutable@ i8)) i32))
    (strtol ((imply str pointer) as (mutable@ i8)) null 10)

fn load-level (file)
    local board : BoardState
    local tokens = (parse-csv (bottle.io.load-file-string file))

    local width = (string->num ('remove tokens 0))
    local height = (string->num ('remove tokens 0))
    board.dimensions = (ivec2 width height)

    fold (x y = 0 0) for token in tokens
        local c = (token @ 0)
        switch c
        case (char "#")
            'append board.tiles TileType.Wall
        case (char "-")
            'append board.tiles TileType.Free
        case (char "G")
            'append board.tiles TileType.Goal
        case (char "P")
            'append board.tiles TileType.Free
            board.player = (ivec2 x y)
        case (char "~")
            'append board.tiles TileType.Fragile
        case (char "b")
            'append board.tiles TileType.Free
            'append board.boxes (ivec2 x y) 
        case (char "B")
            'append board.tiles TileType.Free
            local bomb : Bomb 
            bomb.pos = (ivec2 x y)
            bomb.timer = (string->num (& (token @ 1))) as u32
            'append board.bombs bomb
        default
            print token
            assert false "unrecognized tile type"
            unreachable;
        
        if (x == (width - 1))
            _ 0 (y + 1)
        else
            _ (x + 1) y
    deref board

do 
    let 
        load-level
        BoardState
        Bomb
        TileType    
    locals;

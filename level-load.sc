import .bottle
using import glm
using import String
using import UTF-8
using import struct 
using import enum
using import Array
using import itertools

enum TileType plain
    Free
    Wall
    Fragile
    Goal

    inline __typecall (cls)
        this-type.Free

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
    local values : (Array String)
    local value : String
    # not UTF-8 aware.
    loop (idx = 0)
        if (idx == (countof input))
            'append values (copy value)
            break;
        c := input @ idx

        if (c == (char32 ","))
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
    local tokens = (parse-csv ('force-unwrap (bottle.io.load-file-string file)))

    local width = (string->num ('remove tokens 0))
    local height = (string->num ('remove tokens 0))
    board.dimensions = (ivec2 width height)
    'resize board.tiles (width * height)

    for i x y in (enumerate (dim width height))
        let token = (tokens @ i)
        local c = (token @ 0)
        local index = ((height - y - 1) * width + x)
        switch c
        case (char32 "#")
            board.tiles @ index = TileType.Wall
        case (char32 "-")
            board.tiles @ index = TileType.Free
        case (char32 "G")
            board.tiles @ index = TileType.Goal
        case (char32 "P")
            board.tiles @ index = TileType.Free
            board.player = (ivec2 x (height - y - 1))
        case (char32 "~")
            board.tiles @ index = TileType.Fragile
        case (char32 "b")
            board.tiles @ index = TileType.Free
            'append board.boxes (ivec2 x (height - y - 1)) 
        case (char32 "B")
            board.tiles @ index = TileType.Free
            local bomb : Bomb 
            bomb.pos = (ivec2 x (height - y - 1))
            bomb.timer = (string->num (& (token @ 1))) as u32
            'append board.bombs bomb
        default
            assert false "unrecognized tile type"
            unreachable;
    deref board

do 
    let 
        load-level
        BoardState
        Bomb
        TileType    
    locals;

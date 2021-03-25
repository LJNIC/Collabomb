-- format is data separated by commas:
-- level width
-- level height
-- ground tile layer
--    - = floor (indicates free passage)
--    # = wall (blocked passage)
--    G = goal
-- object layer
--    ~ = destructible wall (can be blown up)
--    b = box
--    B(n) = bomb, where n is an arbitrary long number of ticks remaining for explosion
--    P = player

local slab = require "Slab"

function love.load (args)
    slab.Initialize(args)
    love.graphics.setBackgroundColor(0.14,0.14,0.14)
end

local level_width = 0
local level_height = 0
local level_origin = {0,0}
local SQ_SIZE = 40

local board = {}
local function init_board()
    board = {}
    for y=0, level_height-1 do
        for x=0, level_width-1 do
            board[y * level_width + x + 1] = "-"
        end
    end
end

local function export_board ()
    local level_text = string.format("%d,%d,", level_width, level_height)
    for k,v in ipairs(board) do
        level_text = level_text .. v .. ","
    end
    love.system.setClipboardText(level_text)
    love.window.showMessageBox("Exported!", "Your level data was copied to your clipboard.")
end

local function set_square (x,y, value)
    board[y * level_width + x + 1] = value
end

local function tool_floor (x, y)
   set_square(x,y, "-") 
end

local function tool_wall (x, y)
   set_square(x,y, "#") 
end

local function tool_goal (x, y)
   set_square(x,y, "G") 
end

local function tool_dwall (x, y)
   set_square(x,y, "~") 
end

local function tool_box (x, y)
   set_square(x,y, "b") 
end

local selected_bomb_timer = 0
local function tool_bomb (x, y)
   set_square(x,y, "B" .. tostring(selected_bomb_timer)) 
end

local last_player
local function tool_player (x, y)
    if last_player then
        local px,py = unpack(last_player)
        set_square(px,py, "-")
    end
    set_square(x,y, "P")
    last_player = {x,y}
end

local tools = {
    tool_floor,
    tool_wall,
    tool_goal,
    tool_dwall,
    tool_box,
    tool_bomb,
    tool_player,
    ---------------------
    [tool_floor] = "floor",
    [tool_wall] = "wall",
    [tool_goal] = "goal",
    [tool_dwall] = "dwall",
    [tool_box] = "box",
    [tool_bomb] = "bomb",
    [tool_player] = "player",
}

local current_tool = tools[1]

local selected_bomb_timer_text = ""
local bomb_timer_last_typed = love.timer.getTime()
function love.textinput (t)
    -- if you have the bomb selected, you can type to change the timer, it will stick.
    if current_tool == tool_bomb then
        if not ((love.timer.getTime() - bomb_timer_last_typed) < 0.5) then
            selected_bomb_timer_text = ""
        end
        if tonumber(t) then
            selected_bomb_timer_text = selected_bomb_timer_text .. t
            selected_bomb_timer = tonumber(selected_bomb_timer_text)
            bomb_timer_last_typed = love.timer.getTime()
        end
    end
end

local panning = false
local pan_start = {0,0}
function love.mousepressed(x,y,b)
    if b == 2 then
        pan_start = {x,y}
        panning = true
    end
end

function love.mousereleased(x,y,b)
    if b == 2 then
        local lx,ly = unpack(level_origin)
        local px,py = x - pan_start[1], y - pan_start[2]
        lx, ly = lx + px, ly + py
        level_origin = {lx,ly}
        panning = false
    end
end

local function current_tile ()
    local mx, my = love.mouse.getPosition()
    local lx, ly = unpack(level_origin)
    return math.floor((mx - lx) / SQ_SIZE), math.floor((my - ly) / SQ_SIZE)
end

function love.update(dt)
	slab.Update(dt)
  
    slab.BeginWindow('Level Size', {Title = "Level Size"})
    slab.Text("width")
    if slab.Input("level_width", {Text = tostring(level_width)}) then
        local n = tonumber(slab.GetInputText("level_width"))
        level_width = math.floor(n or 0)
        init_board()
    end
    slab.Text("height")
    if slab.Input("level_height", {Text = tostring(level_height)}) then
        local n = tonumber(slab.GetInputText("level_height"))
        level_height = math.floor(n or 0)
        init_board()
    end
    slab.EndWindow()

	slab.BeginWindow('Tools', {Title = "Tools"})
    slab.Text("Current: " .. tostring(tools[current_tool]))
    slab.Text("Ground")
    if slab.Button("floor") then
        current_tool = tools[1]
    end
    if slab.Button("wall") then
        current_tool = tools[2]
    end
    if slab.Button("goal") then
        current_tool = tools[3]
    end

    slab.Separator()
    slab.Text("Objects")
    if slab.Button("d. wall") then
        current_tool = tools[4]
    end
    if slab.Button("box") then
        current_tool = tools[5]
    end
    if slab.Button("bomb (" .. tostring(selected_bomb_timer) .. ")") then
        current_tool = tools[6]
    end
    if slab.Button("player") then
        current_tool = tools[7]
    end

    slab.Separator()
    if slab.Button("Export") then
        export_board()
    end
	slab.EndWindow()
    if not panning and love.mouse.isDown(1) then
        local sqx, sqy = current_tile()
        if sqx >= 0 and sqx < level_width and sqy >= 0 and sqy < level_height then
            current_tool(sqx, sqy)
        end
    end
end

function love.draw()
    local lx,ly = unpack(level_origin)
    -- animate panning
    if panning then
        local mx,my = love.mouse.getPosition()
        local px,py = mx - pan_start[1], my - pan_start[2]
        lx, ly = lx + px, ly + py
    end

    for x=0,level_width do
        love.graphics.line(lx+x*SQ_SIZE, ly, lx+x*SQ_SIZE, ly+level_height*SQ_SIZE)
    end
    for y=0,level_height do
        love.graphics.line(lx, ly+y*SQ_SIZE, lx+level_width*SQ_SIZE, ly+y*SQ_SIZE)
    end

    -- selected square
    if not panning then
        local sqx, sqy = current_tile()
        if sqx >= 0 and sqx < level_width and sqy >= 0 and sqy < level_height then
            love.graphics.rectangle('fill', lx + sqx * SQ_SIZE, ly + sqy * SQ_SIZE, SQ_SIZE, SQ_SIZE)
        end

    end
    -- square info
    for y=0, level_height-1 do
        for x=0, level_width-1 do
            local sqx, sqy = current_tile()
            love.graphics.print(board[y * level_width + x + 1], lx + x * SQ_SIZE + 5, ly + y * SQ_SIZE + 5)
        end
    end

	slab.Draw()
end

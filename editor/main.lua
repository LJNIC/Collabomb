-- format is data separated by commas:
-- level width
-- level height
-- ground tile layer
--    - = floor (indicates free passage)
--    # = wall (blocked passage)
--    . = goal
-- object layer
--    ~ = destructible wall (can be blown up)
--    b = box
--    B(n) = bomb, where n is an arbitrary long number of ticks remaining for explosion
--    P = player

local slab = require "Slab"

function love.load (args)
    slab.Initialize(args)
end

local function tool_floor (x, y)
end

local function tool_wall (x, y)
end

local function tool_goal (x, y)
end

local function tool_dwall (x, y)
end

local function tool_box (x, y)
end

local function tool_bomb (x, y)
end

local function tool_player (x, y)
end

local function tool_pipette (x, y)
end

local tools = {
    tool_floor,
    tool_wall,
    tool_goal,
    tool_dwall,
    tool_box,
    tool_bomb,
    tool_player,
    tool_pipette,
    ---------------------
    [tool_floor] = "floor",
    [tool_wall] = "wall",
    [tool_goal] = "goal",
    [tool_dwall] = "dwall",
    [tool_box] = "box",
    [tool_bomb] = "bomb",
    [tool_player] = "player",
    [tool_pipette] = "pipette"
}

local current_tool = tools[1]
local level_width = 0
local level_height = 0

local selected_bomb_timer_text = ""
local bomb_timer_last_typed = love.timer.getTime()
local selected_bomb_timer = 0
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

function love.update(dt)
	slab.Update(dt)
  
    slab.BeginWindow('Level Size', {Title = "Level Size"})
    slab.Text("width")
    if slab.Input("level_width", {Text = tostring(level_width)}) then
        local n = tonumber(slab.GetInputText("level_width"))
        level_width = math.floor(n or 0)
    end
    slab.Text("height")
    if slab.Input("level_height", {Text = tostring(level_height)}) then
        local n = tonumber(slab.GetInputText("level_height"))
        level_height = math.floor(n or 0)
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
    if slab.Button("pipette") then
        current_tool = tools[8]
    end
	slab.EndWindow()

    -- tool behaviour
end

function love.draw()
	slab.Draw()
end

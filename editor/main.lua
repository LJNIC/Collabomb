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

function love.update(dt)
	slab.Update(dt)
  
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
    if slab.Button("bomb") then
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
end

function love.draw()
	slab.Draw()
end

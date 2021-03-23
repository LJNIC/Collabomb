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

function love.update(dt)
	slab.Update(dt)
  
	slab.BeginWindow('MyFirstWindow', {Title = "My First Window"})
	slab.Text("Hello World")
	slab.EndWindow()
end

function love.draw()
	slab.Draw()
end

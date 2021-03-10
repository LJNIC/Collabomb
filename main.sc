using import glm

import .bottle
from bottle.graphics let Sprite

@@ 'on bottle.config
fn (cfg)
    cfg

global spr : Sprite
global pos = (vec2 250 250)

@@ 'on bottle.load
fn ()
    spr = (Sprite "square.png")

@@ 'on bottle.update
fn ()
    if (bottle.input.down? 'Left)
        pos.x -= 10.0
    if (bottle.input.down? 'Right)
        pos.x += 10.0
    if (bottle.input.down? 'Down)
        pos.y -= 10.0
    if (bottle.input.down? 'Up)
        pos.y += 10.0

@@ 'on bottle.draw
fn ()
    bottle.graphics.sprite spr (vec2 pos.x pos.y)

bottle.run;


# Collabomb
A sokoban-like with bombs. Instead of pushing blocks around, you push around bombs. Bombs might have a (turn-based) timer, or they might have to be detonated in a specific way. Bombs might also have different effects when they explode (think bomberman?). The goal of a level will be to blow up a specific tile with a bomb.

## Questions
* What can the player do?
* What are all of the bomb modifiers?
* Game theme? Where are we? Why are we blowing things up? Or is it abstract?

## Mechanics
* Boxes
  * Act as normal Sokoban boxes
  * Can be blown up by bombs
* Bombs
  * Can be pushed likes boxes, but have a timer
  * When the timer runs out, the bomb explodes, hitting orthogonally adjacent tiles
  * Bombs can have "infinite" timers and only explode when hit by another bomb
* Destructible Walls
  * Walls that get destroyed by bombs

## Winning Rule
All boxes must be on goal tiles. Boxes can be blown up, meaning they won't have to be on goals.

## Puzzle Ideas
* Basic Sokoban
* Pushing bombs to blow up walls that unlock goals or boxes
* Chains of infinite timer bombs
  * Pushing blocks next to a chain so they get blown up
  * Pushing bombs into chains to continue it
  * Pushing bombs out of chains

execute as @a unless data entity @s Inventory[{tag:{Zauberstab:1b}}] run function zauberstab:give_wand

execute as @a if score @s zs_use > @s zs_use_prev run function zauberstab:wand_right_click
execute as @a run scoreboard players operation @s zs_use_prev = @s zs_use

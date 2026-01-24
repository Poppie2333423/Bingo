execute as @a[tag=!bingo_wand_given] run function bingo:give_wand

execute as @a if data entity @s SelectedItem.tag.bingo_wand run function bingo:handle_wand

scoreboard players operation @a wand_use_prev = @a wand_use
scoreboard players operation @a wand_attack_prev = @a wand_attack

# Bingo

## Zauberstab-Datapack (Minecraft 1.21 + Pehkui)

Dieses Repository enthält ein Datapack, das jedem Spieler automatisch einen Zauberstab gibt (und ihn bei Verlust erneut vergibt) und die Größe über die Pehkui-Mod steuert:

- **Rechtsklick** mit dem Zauberstab: 0,1 Schritte kleiner.
- **Linksklick** (ein Entity treffen) mit dem Zauberstab: 0,1 Schritte größer.

### Installation

1. Lege den Ordner `zauberstab_datapack` in deinen Weltordner unter `datapacks/`.
2. Stelle sicher, dass die **Pehkui**-Mod installiert ist (Client + Server).
3. Starte die Welt neu oder nutze `/reload`.

### Hinweis

Der Linksklick wird nur erkannt, wenn du mit dem Zauberstab ein Entity triffst (Minecraft-Trigger `player_hurt_entity`).

# Bingo Zauberstab Plugin

Dieses Projekt ist ein Fabric-Server-Plugin (Mod) für Minecraft 1.21 mit Pehkui.

## Features
- Gibt Spielern beim ersten Join automatisch einen Zauberstab (Karotte am Stock).
- Rechtsklick verkleinert den Spieler um `0.1`, Linksklick vergrößert ihn um `0.1`.
- Optionaler Befehl zur manuellen Vergabe: `/zauberstab [targets]`.

## Build
Dieses Projekt nutzt **Maven**. Baue es daher mit:

```bash
mvn clean package
```

Das fertige JAR liegt danach unter `target/`.

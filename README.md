<div align="center">
  <img src="src/main/resources/pack.png" width="120" alt="Visuality icon"/>

  <h1>Visuality: Reforged (1.12.2 Forge)</h1>

  <p>Little visual improvements that add a bunch of new ambient particles. Client side and cosmetic only.</p>
</div>

A 1.12.2 port of Visuality, originally by PinkGoosik (Fabric) and ported to
modern Forge as Visuality: Reforged by LimonBlaze. The look and behaviour of the original are
preserved, rebuilt on the 1.12.2.

## Features

- **Ore sparkles.** Diamond, gold, emerald, lapis, and redstone ore give off gem coloured sparkles.
- **Armor sparkles.** Entities wearing diamond or gold armor sparkle in the matching colour.
- **Soul particles.** Soul sand gives off rising soul wisps.
- **Slime blobs.** Slimes throw out coloured blobs when they land.
- **Charge particles.** Charged creepers spark.
- **Hit particles.** Skeletons drop bones, wither skeletons drop wither bones, chickens drop
  feathers, and villagers drop emeralds when hit. The amount scales with the attacker's weapon damage.
- **Water crowns.** Rain raises small biome tinted ripples on open water.

Everything is client side.

## Requirements

- Minecraft 1.12.2
- Minecraft Forge 14.23.5.2860 or newer
- [MixinBooter](https://www.curseforge.com/minecraft/mc-mods/mixinbooter) (the mod loads its mixins through MixinBooter)

## Installation

1. Install Minecraft Forge for 1.12.2.
2. Drop MixinBooter and `visuality-1.1.0.jar` into your `mods` folder.
3. Launch the game.

## Known issues

- **Water crowns at grazing angles.** Water crown particles render as a flat quad on the block
  texture atlas. Viewed from a very shallow angle they can darken, because at that angle the GPU
  samples high mipmap levels of a small atlas sprite. They look correct from above and up close.
  Candidate fixes are tracked for a future update.

## License

[MIT](LICENSE). The original mod and the Reforged port are MIT licensed; this port keeps the same license.

# Third-party assets

SwiftDM does not include Apple assets or SF Pro font files. The interface only
follows the public Human Interface Guidelines for spacing, colour and motion.

## Inter

- Source: https://github.com/google/fonts/tree/main/ofl/inter (upstream https://rsms.me/inter/)
- License: SIL Open Font License 1.1, see `Inter-OFL.txt`
- Bundled files: `inter_regular.ttf`, `inter_medium.ttf`, `inter_semibold.ttf`,
  `inter_bold.ttf`
- Change: the upstream variable font was instantiated at weights 400 / 500 /
  600 / 700 (optical size 17). Glyph outlines are unchanged.

## Material Symbols Rounded

- Source: https://github.com/google/material-design-icons
- License: Apache License 2.0, see `MaterialSymbols-LICENSE.txt`
- Bundled file: `material_symbols_rounded.ttf`
- Change: static instance at weight 400, FILL 0, GRAD 0, optical size 24, then
  subsetted to the 62 glyphs SwiftDM uses. Codepoints follow the upstream
  `.codepoints` metadata.

Material Symbols Rounded © Google, licensed under the Apache License 2.0.

# Mochy Music 8.4.2 port

This repository is a modified, literal source port of Mochy Music 8.4.2 for Android.

- Upstream project: Mochy Music — https://github.com/realidkroo/Mochy-Music
- Upstream commit: `7e590802345329bb108d74f337af1ea9fe407d2a`
- Upstream release studied: `Mochy-8.4.2` / `8.4.2 build#1261`

## Licensing and provenance

The Mochy Music source is distributed under the GNU General Public License,
version 3. The upstream GPL-3.0 license text is preserved in `LICENSE`, and
upstream copyright, license, and attribution notices remain in the source and
documentation.

The ARMz-derived segmented/resumable download engine and its surrounding data,
DI, and test code were originally developed under the MIT License by Rezerox39.
Those contributions are relicensed here under GPL-3.0 as part of this combined
work. No additional restrictions are placed on this GPL-3.0 port.

This port keeps Mochy's `com.mochyy.mochymusic` Kotlin namespace, branding,
resources, navigation, screens, and animations. Its base Android application id
is `com.abhi.armz` so the preserved AMz downloads entry point remains installable
under the ARMz identity; Mochy's upstream debug suffix produces
`com.abhi.armz.debug` for debug builds.

See `THIRD_PARTY_NOTICES.md` for bundled dependency notices and
`docs/superpowers/specs/2026-11-17-literal-mochy-port-design.md` for the port
design and scope decisions.

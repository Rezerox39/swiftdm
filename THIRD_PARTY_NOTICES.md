# Third-party notices

This port is licensed as a whole under the GNU General Public License, version 3
(see `LICENSE`). Each third-party component listed below remains subject to its
own license terms; nothing here grants rights beyond those terms.

## Upstream application

- **Mochy Music** — https://github.com/realidkroo/Mochy-Music
- Pinned source commit: `7e590802345329bb108d74f337af1ea9fe407d2a`
- Studied release: `Mochy-8.4.2` / `8.4.2 build#1261`
- License: GPL-3.0. Upstream source, resources, and notices are preserved in
  this repository.

## Repository license

- This combined work, including the relicensed ARMz download engine, is
  distributed under GPL-3.0. See `LICENSE` and `NOTICE.md`.

## Major Android dependency families from Mochy's version catalog

These families are declared in `gradle/libs.versions.toml` and retain their
respective upstream licenses:

- Jetpack Compose and Activity Compose
- AndroidX Media3
- Dagger Hilt
- AndroidX Room
- Ktor
- Coil
- Lottie
- Kotlin, Kotlin Coroutines, KotlinX Serialization
- OkHttp and other networking dependencies
- Material libraries and AndroidX core libraries

Consult each dependency's published license and source distribution for its
exact terms. The dependency list above is descriptive, not an exhaustive legal
inventory of every transitive artifact.

## Removed material

The former React/WebView ARMz UI, including React Bits components and their
MIT + Commons Clause notices, is **not shipped** in this port. That material
remains only in earlier Git history for provenance and is not part of the
current source tree or built APK.

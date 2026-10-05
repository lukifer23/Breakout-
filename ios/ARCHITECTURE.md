# iOS architecture (source inspection)

SwiftUI views and GameViewModel drive a SpriteKit GameScene with a separate Swift
GameEngine. Models, level generation and local UserDefaults services are native
and do not share the Android simulation. This inspection does not establish
runtime parity or a current Xcode build.

Android's new seeded RNG, daily transaction and active-run schemas have not been
ported. See [platform parity](../Docs/PARITY.md) and [Android architecture](../Docs/ARCHITECTURE.md).
The macOS fork is archived/experimental. Historic design spikes are under Archive.

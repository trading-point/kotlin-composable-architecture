# The Komposable Architecture

A Kotlin port of [Point Free](https://github.com/pointfreeco)'s "[The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture)".

## Design decisions
- Effect is just a `typealias` of `Observable`, that way no type erasure is required. 
- Since there are no key paths in Kotlin, the concept of Optics was used to abstract read/write to
- Since there are no key paths (`\.`) in Kotlin, the concept of Optics was used to abstract read/write to
  immutable data structures.
  For more info on the concept please check [Arrow Optics](https://arrow-kt.io/docs/0.10/optics/lens/).
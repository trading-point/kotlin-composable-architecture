# The Komposable Architecture

A Kotlin port of [Point Free](https://github.com/pointfreeco)'s "[The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture)".

## Design decisions
- Effect is just a `typealias` of `Observable`, that way no type erasure is required. 
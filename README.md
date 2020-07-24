# The Komposable Architecture

A Kotlin port of [Point Free](https://github.com/pointfreeco)'s "[The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture)".

## Design decisions
- Effect is just a `typealias` of `Observable`, that way no type erasure is required. 
- Since the state is immutable we were forced to return a result from the `reduce` function 
  (named `Reduced`) that would hold the new state along with it's Effect (`Reduced` is actually a 
  `typealias` of `Pair<STATE, Effect<ACTION>`). 
- Since there are no key paths (`\.`) in Kotlin, the concept of Optics was used to abstract read/write to
  immutable data structures.
  For more info on the concept please check [Arrow Optics](https://arrow-kt.io/docs/0.10/optics/lens/).
- Because of the different lifecycle of objects in the JVM world and the absence of `deinit` to dispose
  the subscriptions the `Store` contract was changed a bit to accept a parent stream and merge it to
  the internal `State` stream. That way no disposable is being held within the `Store` and only one
  stream is exposed whose lifecycle need to be handled on call-site. 

## Pending items:
- Migrate `forEach` reducers
_ Migrate `ViewStore.binding`
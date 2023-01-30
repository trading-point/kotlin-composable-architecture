# A Kotlin port of The Composable Architecture (aka **TKA**)

⚠️ THERE ARE STILL SEVERAL ROUGH EDGES SO USE IT AT YOUR OWN RISK ⚠️

[Point-Free's](https://github.com/pointfreeco) [The Composable Architecture](https://github.com/pointfreeco/swift-composable-architecture) is a Swift library so this "fork" has ported the core concepts in Kotlin in order to help share domain logic amongst Android/iOS apps.

## Design decisions
- As with every Kotlin library that respect itself, the name needs to be paraphrased using the **K** and thus, **The Komposable Architecture** (aka **TKA**) was born.
- Why RxJava? Because the nature of our project dictated the use of a reactive framework and at the time of adoption (2020) it was the only stable and production ready one for Android.
- The `tka` module is a pure Kotlin library module with no platform dependencies so that it can be used on any java/kotlin project.
- Effect is just a `typealias` of `Observable`, that way no type erasure is required. 
- Since the state is immutable and there is no `inout` in Kotlin, the `reduce` function returns a `Reduced` result that holds the new (copied) state along with it's `Effect`. 
- Since there are no key/case paths (`\.`) in Kotlin, the concept of **Optics** was used to abstract read/write operations of immutable data structures.
  For more info on the concept please check [Arrow Optics](https://arrow-kt.io/docs/0.10/optics/lens/).
- Because of the different lifecycle of objects in the JVM world and the absence of `deinit` to dispose
  the subscriptions, the `Store` contract was changed a bit to accept a parent stream and merge it to
  the internal `State` stream. That way no disposable is being held within the `Store` and only one
  stream is exposed whose lifecycle need to be handled on call-site. 

## TODO
- Migrate Debugging tools
- Migrate more Examples 😅
- Migrate `forEach` reducers

## Future work
- Add coroutines support
- Add Arrow supplementary package to utilize the power of Arrow Optics and Arrow Meta
- Add supplementary modules for bridging with Android Jetpack
- Investigate Kotlin/Native porting

# The Composable Architecture

The Composable Architecture is a library for building applications in a consistent and understandable way, with composition, testing, and ergonomics in mind. It can be used in SwiftUI, UIKit, and more, and on any Apple platform (iOS, macOS, tvOS, and watchOS) **and now on any JVM platform as well**

- [A Kotlin port of The Composable Architecture (aka **TKA**)](#a-kotlin-port-of-the-composable-architecture-aka-tka)
  - [Design decisions](#design-decisions)
  - [TODO](#todo)
  - [Future work](#future-work)
- [The Composable Architecture](#the-composable-architecture)
  - [What is the Composable Architecture?](#what-is-the-composable-architecture)
  - [Learn More](#learn-more)
  - [Examples](#examples)
  - [Basic Usage](#basic-usage)
    - [Testing](#testing)
    - [Debugging](#debugging)
  - [FAQ](#faq)
  - [Requirements](#requirements)
  - [Help](#help)
  - [Credits and thanks](#credits-and-thanks)
  - [Other libraries](#other-libraries)
  - [License](#license)

## What is the Composable Architecture?

This library provides a few core tools that can be used to build applications of varying purpose and complexity. It provides compelling stories that you can follow to solve many problems you encounter day-to-day when building applications, such as:

* **State management**
  <br> How to manage the state of your application using simple value types, and share state across many screens so that mutations in one screen can be immediately observed in another screen.

* **Composition**
  <br> How to break down large features into smaller components that can be extracted to their own, isolated modules and be easily glued back together to form the feature.

* **Side effects**
  <br> How to let certain parts of the application talk to the outside world in the most testable and understandable way possible.

* **Testing**
  <br> How to not only test a feature built in the architecture, but also write integration tests for features that have been composed of many parts, and write end-to-end tests to understand how side effects influence your application. This allows you to make strong guarantees that your business logic is running in the way you expect.

* **Ergonomics**
  <br> How to accomplish all of the above in a simple API with as few concepts and moving parts as possible.

## Learn More

The Composable Architecture was designed over the course of many episodes on [Point-Free](https://www.pointfree.co), a video series exploring functional programming and the Swift language, hosted by [Brandon Williams](https://twitter.com/mbrandonw) and [Stephen Celis](https://twitter.com/stephencelis).

You can watch all of the episodes [here](https://www.pointfree.co/collections/composable-architecture), as well as a dedicated, multipart tour of the architecture from scratch: [part 1](https://www.pointfree.co/collections/composable-architecture/a-tour-of-the-composable-architecture/ep100-a-tour-of-the-composable-architecture-part-1), [part 2](https://www.pointfree.co/collections/composable-architecture/a-tour-of-the-composable-architecture/ep101-a-tour-of-the-composable-architecture-part-2), [part 3](https://www.pointfree.co/collections/composable-architecture/a-tour-of-the-composable-architecture/ep102-a-tour-of-the-composable-architecture-part-3) and [part 4](https://www.pointfree.co/collections/composable-architecture/a-tour-of-the-composable-architecture/ep103-a-tour-of-the-composable-architecture-part-4).

<a href="https://www.pointfree.co/collections/composable-architecture">
  <img alt="video poster image" src="https://i.vimeocdn.com/video/850265054.jpg" width="600">
</a>

## Examples

No examples have been ported yet but you can always take a look at the [original ones](https://github.com/pointfreeco/swift-composable-architecture#examples)!

<!-- [![Screen shots of example applications](https://d3rccdn33rt8ze.cloudfront.net/composable-architecture/demos.png)](./Examples)

This repo comes with _lots_ of examples to demonstrate how to solve common and complex problems with the Composable Architecture. Check out [this](./Examples) directory to see them all, including:

* [Case Studies](./Examples/CaseStudies)
  * Getting started
  * Effects
  * Navigation
  * Higher-order reducers
  * Reusable components
* [Location manager](./Examples/LocationManager)
* [Motion manager](./Examples/MotionManager)
* [Search](./Examples/Search)
* [Speech Recognition](./Examples/SpeechRecognition)
* [Tic-Tac-Toe](./Examples/TicTacToe)
* [Todos](./Examples/Todos)
* [Voice memos](./Examples/VoiceMemos) -->

## Basic Usage

To build a feature using the Composable Architecture you define some types and values that model your domain:

* **State**: A type that describes the data your feature needs to perform its logic and render its UI.
* **Action**: A type that represents all of the actions that can happen in your feature, such as user actions, notifications, event sources and more.
* **Environment**: A type that holds any dependencies the feature needs, such as API clients, analytics clients, etc.
* **Reducer**: A function that describes how to evolve the current state of the app to the next state given an action. The reducer is also responsible for returning any effects that should be run, such as API requests, which can be done by returning an `Effect` value.
* **Store**: The runtime that actually drives your feature. You send all user actions to the store so that the store can run the reducer and effects, and you can observe state changes in the store so that you can update UI.

The benefits of doing this is that you will instantly unlock testability of your feature, and you will be able to break large, complex features into smaller domains that can be glued together.

As a basic example, consider a UI that shows a number along with "+" and "−" buttons that increment and decrement the number. To make things interesting, suppose there is also a button that when tapped makes an API request to fetch a random fact about that number and then displays the fact in an alert.

The state of this feature would consist of an integer for the current count, as well as an optional string that represents the title of the alert we want to show (optional because `null` represents not showing an alert):

```kotlin
data class AppState(
    val count: Int = 0,
    val numberFactAlert: String? = null
)
```

Next we have the actions in the feature. There are the obvious actions, such as tapping the decrement button, increment button, or fact button. But there are also some slightly non-obvious ones, such as the action of the user dismissing the alert, and the action that occurs when we receive a response from the fact API request:

```kotlin
sealed class AppAction {
    object FactAlertDismissed : AppAction()
    object DecrementButtonTapped : AppAction()
    object IncrementButtonTapped : AppAction()
    object NumberFactButtonTapped : AppAction()
    data class NumberFactResponse(val result: Result<String>) : AppAction()
}

sealed class Result<out SUCCESS, out ERROR> {
    data class Success<SUCCESS>(val value: SUCCESS) : Result<SUCCESS, Nothing>()
    data class Error<ERROR>(val value: ERROR) : Result<Nothing, ERROR>()
}

class ApiError : Error()
```

Next we model the environment of dependencies this feature needs to do its job. In particular, to fetch a number fact we need to construct an `Effect` value that encapsulates the network request. So that dependency is a function from `Int` to `Effect<String>`, where `String` represents the response from the request. Further, the effect will typically do its work on a background thread and so we need a way to receive the effect's values on the main thread. We do this via scheduler dependencies that are important to control so that we can write tests. We must use a `Scheduler` so that we can use, for example, `Schedulers.io()` for background and `AndroidSchedulers.mainThread()` for main in production and a test scheduler in tests.

```kotlin
interface AppEnvironment {
    var backgroundScheduler: Scheduler
    var mainScheduler: Scheduler
    var numberFact: (Int) -> Effect<String>
}
```

Next, we implement a reducer that implements the logic for this domain. It describes how to change the current state to the next state, and describes what effects need to be executed. Some actions don't need to execute effects, and they can return `none()` to represent that:

```kotlin
val appReducer = Reducer<AppState, AppAction, AppEnvironment> { state, action, environment ->
    when (action) {
        AppAction.FactAlertDismissed -> state.copy(numberFactAlert = null) + none()
        AppAction.DecrementButtonTapped -> state.copy(count = state.count - 1) + none()
        AppAction.IncrementButtonTapped -> state.copy(count = state.count + 1) + none()
        AppAction.NumberFactButtonTapped -> state + environment.numberFact(state.count)
            .subscribeOn(environment.backgroundScheduler)
            .map<AppAction> { AppAction.NumberFactResponse(Result.Success(it)) }
            .onErrorReturn { AppAction.NumberFactResponse(Result.Error(ApiError(it))) }
            .observeOn(environment.mainScheduler)
        is AppAction.NumberFactResponse -> when (val result = action.result) {
            is Result.Success -> state.copy(numberFactAlert = result.value) + none()
            is Result.Error -> state.copy(numberFactAlert = "Could not load a number fact :(") + none()
        }
    }
}
```

And then finally we define the view that displays the feature. It holds onto a `Store<AppState, AppAction>` so that it can observe all changes to the state and re-render, and we can send all user actions to the store so that state changes:

```kotlin
class AppView(
  private val store: Store<AppState, AppAction>
) : Fragment(R.layout.app_view) {

  private var viewStoreDisposable: Disposable? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val viewStore = store.view()

    viewStoreDisposable = viewStore.states
      .subscribe { appState ->
        textView.text = "${appState.count}"

        appState.numberFactAlert
          ?.also {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()

            viewStore.send(AppAction.FactAlertDismissed)
          }
      }

    decrButton.setOnClickListener { viewStore.send(AppAction.DecrementButtonTapped) }
    incrButton.setOnClickListener { viewStore.send(AppAction.IncrementButtonTapped) }
    factButton.setOnClickListener { viewStore.send(AppAction.NumberFactButtonTapped) }
  }

  override fun onDestroyView() {
    viewStoreDisposable?.dispose()
    super.onDestroyView()
  }
}
```

It's important to note that we were able to implement this entire feature without having a real, live effect at hand. This is important because it means features can be built in isolation without building their dependencies, which can help compile times.

Once we are ready to display this view, we can construct a store. This is the moment where we need to supply the dependencies, and for now we can just use an effect that immediately returns a mocked string:

```kotlin
val appstore = Store(
    initialState = AppState(),
    reducer = appReducer,
    environment = object : AppEnvironment {
        override var backgroundScheduler: Scheduler = Schedulers.io()
        override var mainScheduler: Scheduler = AndroidSchedulers.mainThread()
        override var numberFact: (Int) -> Effect<String> = {
            Effects.just("$it is a good number Brent")
        }
    }
)
```

And that is enough to get something on the screen to play around with. It gives us a consistent manner to apply state mutations, instead of scattering logic in some observable objects and in various action closures of UI components. It also gives us a concise way of expressing side effects. And we can immediately test this logic, including the effects, without doing much additional work.

### Testing

To test, you first create a `TestStore` with the same information that you would to create a regular `Store`, except this time we can supply test-friendly dependencies. In particular, we use a test scheduler instead of the live scheduler because that allows us to control when work is executed, and we don't have to artificially wait for queues to catch up.

```kotlin
val scheduler = TestScheduler()

val store = TestStore(
    initialState = AppState(),
    reducer = appReducer,
    environment = object : AppEnvironment {
        override var backgroundScheduler: Scheduler = TestScheduler()
        override var mainScheduler: Scheduler = TestScheduler()
        override var numberFact: (Int) -> Effect<String> = {
            Effects.just("$it is a good number Brent")
        }
    }
)
```

Once the test store is created we can use it to make an assertion of an entire user flow of steps. Each step of the way we need to prove that state changed how we expect. Further, if a step causes an effect to be executed, which feeds data back into the store, we must assert that those actions were received properly.

The test below has the user increment and decrement the count, then they ask for a number fact, and the response of that effect triggers an alert to be shown, and then dismissing the alert causes the alert to go away.

```kotlin
store.assert {
    // Test that tapping on the increment/decrement buttons changes the count
    send(AppAction.IncrementButtonTapped) {
        it.copy(count = 1)
    }
    send(AppAction.DecrementButtonTapped) {
        it.copy(count = 0)
    }

    // Test that tapping the fact button causes us to receive a response from the effect. Note
    // that we have to advance the scheduler because we used `.receiveOn()` in the reducer.
    send(AppAction.NumberFactButtonTapped)
    
    scheduler.triggerActions()
    
    receive(AppAction.NumberFactResponse(Success("0 is a good number Brent"))) {
        it.copy(numberFactAlert = "0 is a good number Brent")
    }

    // And finally dismiss the alert
    send(AppAction.FactAlertDismissed) {
        it.copy(numberFactAlert = null)
    }
}
```

That is the basics of building and testing a feature in the Composable Architecture. There are _a lot_ more things to be explored, such as composition, modularity, adaptability, and complex effects. The **original** [Examples](https://github.com/pointfreeco/swift-composable-architecture/tree/main/Examples) directory has a bunch of projects to explore to see more advanced usages.

### Debugging

No debugging tools have been ported yet... apart from a simple `reducer.debug()` to log the actions the reducer receives and the mutations it makes to the state but without any fancy output (yet).

## FAQ

* How does the Composable Architecture compare to Elm, Redux, and others?
  <details>
    <summary>Expand to see answer</summary>
    The Composable Architecture (TCA) is built on a foundation of ideas popularized by Elm and Redux, but made to feel at home in the Swift language and on Apple's platforms.

    In some ways TCA is a little more opinionated than the other libraries. For example, Redux is not prescriptive with how one executes side effects, but TCA requires all side effects to be modeled in the `Effect` type and returned from the reducer.

    In other ways TCA is a little more lax than the other libraries. For example, Elm controls what kinds of effects can be created via the `Cmd` type, but TCA allows an escape hatch to any kind of effect since `Effect` is just an RxJava `Observable`.

    And then there are certain things that TCA prioritizes highly that are not points of focus for Redux, Elm, or most other libraries. For example, composition is very important aspect of TCA, which is the process of breaking down large features into smaller units that can be glued together. This is accomplished with the `pullback` and `combine` operators on reducers, and it aids in handling complex features as well as modularization for a better-isolated code base and improved compile times.
  </details>

* Why isn't `Store` thread-safe? <br> Why isn't `send` queued? <br> Why isn't `send` run on the main thread?
  <details>
    <summary>Expand to see answer</summary>

    When an action is sent to the `Store`, a reducer is run on the current state, and this process cannot be done from multiple threads. If you are using an effect that may deliver its output on a non-main thread, you must explicitly perform `.observeOn()` in order to force it back on the main thread.

    This approach makes the fewest number of assumptions about how effects are created and transformed, and prevents unnecessary thread hops and re-dispatching. It also provides some testing benefits. If your effects are not responsible for their own scheduling, then in tests all of the effects would run synchronously and immediately. You would not be able to test how multiple in-flight effects interleave with each other and affect the state of your application. However, by leaving scheduling out of the `Store` we get to test these aspects of our effects if we so desire, or we can ignore if we prefer. We have that flexibility.

## Requirements

This port of The Composable Architecture uses the RxJava 3 framework.

<!-- ## Installation

You can add ComposableArchitecture to an Xcode project by adding it as a package dependency.

  1. From the **File** menu, select **Swift Packages › Add Package Dependency…**
  2. Enter "https://github.com/trading-point/reactiveswift-composable-architecture" into the package repository URL text field
  3. Depending on how your project is structured:
      - If you have a single application target that needs access to the library, then add **ComposableArchitecture** directly to your application.
      - If you want to use this library from multiple targets you must create a shared framework that depends on **ComposableArchitecture** and then depend on that framework in all of your targets. For an example of this, check out the [Tic-Tac-Toe](./Examples/TicTacToe) demo application, which splits lots of features into modules and consumes the static library in this fashion using the **TicTacToeCommon** framework. -->

## Help

If you want to discuss the Composable Architecture or have a question about how to use it to solve a particular problem, ask around on [its Swift forum](https://forums.swift.org/c/related-projects/swift-composable-architecture).

## Credits and thanks

The following people gave feedback on the library at its early stages and helped make the library what it is today:

Paul Colton, Kaan Dedeoglu, Matt Diephouse, Josef Doležal, Eimantas, Matthew Johnson, George Kaimakas, Nikita Leonov, Christopher Liscio, Jeffrey Macko, Alejandro Martinez, Shai Mishali, Willis Plummer, Simon-Pierre Roy, Justin Price, Sven A. Schmidt, Kyle Sherman, Petr Šíma, Jasdev Singh, Maxim Smirnov, Ryan Stone, Daniel Hollis Tavares, and all of the [Point-Free](https://www.pointfree.co) subscribers 😁.

Special thanks to [Chris Liscio](https://twitter.com/liscio) who helped us work through many strange SwiftUI quirks and helped refine the final API.

## Other libraries

The Composable Architecture was built on a foundation of ideas started by other libraries, in particular [Elm](https://elm-lang.org) and [Redux](https://redux.js.org/).

## License

This library is released under the MIT license. See [LICENSE](LICENSE) for details.

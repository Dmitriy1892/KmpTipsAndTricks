If you want to create extension function for `plugins {}` block like this:

```kotlin
fun PluginDependenciesSpec.somePlugin(): PluginDependencySpec =
    id("some.plugin")
```

and use it like:

```kotlin
plugins {
    somePlugin()
}
```

You'll see the `Unresolved reference: somePlugin`, for gradle 8.6 and earlier it's impossible:
https://stackoverflow.com/questions/62266477/gradle-custom-function-in-block-plugins
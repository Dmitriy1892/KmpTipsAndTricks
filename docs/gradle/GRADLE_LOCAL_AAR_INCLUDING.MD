Local `.aar` files implementation:

1. Local files implementation like `implementation("libs/bylib.aar")` works with debug builds, but fails on release builds;
2. Local `.aar` files need to implement by proxying it in assembly module:
- in folder, when `.aar` file located, you need to create `build.gradle.kts` file and include the code like below for all build types/flavours:

```kotlin
import com.android.build.api.attributes.BuildTypeAttr

configurations.maybeCreate("debug").attributes { attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(BuildTypeAttr::class.java, "debug")) }

configurations.maybeCreate("release").attributes { attribute(BuildTypeAttr.ATTRIBUTE, project.objects.named(BuildTypeAttr::class.java, "release")) }

artifacts.add("debug", file("mylib.aar"))

artifacts.add("release", file("mylib.aar"))
```

- next, you need to include this module to the `settings.gradle.kts`:

```kotlin
include(":libs")
```

- after that, add this module as dependency to the required modules:

```kotlin
implementation(project(":libs"))
```

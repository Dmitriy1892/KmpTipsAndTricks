# Compose optimizations

1. Stability and equality:
- UI-models must be stable/immutable - don't forget to use `@Stable` or `@Immutable` annotations above the UI models;
- Use `data`-classes for UI-models instead of plain classes, because you don't care about correct realising `equals` and `hashcode` functions for compose checks for UI-models equality in recompositions.

2. Be aware with using big UI-state models, because in that UI-model some fields may to change many times and that can trigger unnecessary recompositions.
You can wrap UI-model into compose `State` and pass wrapped UI-model into composable functions - this way is not recommended by Google, but works in some cases.
Another way - create **state holder** class that wraps your UI-model observable (StateFlow or compose State) and cuts complex state to a separated sub-states:

```kotlin
import javax.swing.plaf.nimbus.State

@Immutable
data class ComplexState(
    val subState1: SubState1,
    val subState2: SubState2,
)

@Stable
class SomeStateHolder internal constructor(
    val someStateFlow: StateFlow<ComplexState>,
) {

    private val subState1Flow = snapshotFlow { someStateFlow.value.subState1 }

    private val subState2Flow = snapshotFlow { someStateFlow.value.subState2 }

    fun collectSubState1WithLifecycle(): State<SubState1>

    fun collectSubState2WithLifecycle(): State<SubState2>

}

@Stable
fun rememberSomeStateHolder(someStateFlow: StateFlow<ComplexState>): SomeStateHolder {
    return remember(someStateFlow) { SomeStateHolder(someStateFlow) }
}
```

Оптимизации:
1. Стабильность и эквивалентность (эквивалентность на data-классах):
- над UI-моделями проставляем @Stable/Immutable, особенно над интерфейсами и sealed классами, т.к. компилятор компоуза считает их нестабильными
- в UI-стейтах используем data class-ы вместо обычных классов, т.к. на data class-ах проще проверяется эквивалентность (equals/hashcode переопределены за нас, эквивалентность проверяется по значению полей)

2. Проброс state до целевой функции - либо в обертке compose state, либо через лямбду

3. Кэшироваине и внимательное отношение к лямбдам:
```
data class Model(
    val title: String,
    …
)
```
- если модель общего стейта меняется часто (Model), но специфичное поле в стейте меняется редко - лучше его обернуть в rememberUpdatedState. Особенно полезно когда лямбда захватывает часть нашего стейта - лямбда не будет пересоздаваться на каждый чих пересоздания стейта (ну или прокидываемый стейт в лямбду обернуть в компоузовский стейт):
```
@Composable
fun Chapter(model: Model, onSelectItem: (String, Int) -> Unit) {
    val title: State<String> = rememberUpdatedState { model.title }
    
    Column {
        Text(item.title.text)
         InnerList(subtitle = section.subtitle.text) { index ->
             onSelectItem.invoke(title.value, index)
         }
    }
}
```

4. Использовать вместо обычных коллекций Immutable-коллекции, либо обернуть в value-классы с @Stable/@Immutable-аннотациями с делегированием:
```
@JvmInline
@Immutable
value class ImmutableList<T>(val data: List<T>): List<T> by data
```

5. Обработка состояний с помощью SnapshotFlow:
```
@Composable
fun AccountsPager(list: ImmutableList<Account>, onPage: (Int) -> Unit) {
    val pagerState = rememberPagerState()

    HorizontalPager(pageCount = list.size, state = pagerState) { page ->
        Column { … }
    }

    // Так нельзя, т.к. LaunchedEffect будет запускаться на каждый чих изменения pagerState.currentPage и ВСЯ функция «AccountsPager» будет рекомпозироваться
    // LaunchedEffect(pagerState.currentPage) { onPage(pagerState.currentPage) }

    LaunchedEffect(pagerState) { // так правильно, т.к. «AccountsPager» не будет рекомпозироваться, но коллбэки в onPage все равно будут отправляться, как и задумано
        snapshotFlow { pagerState.currentPage }
            .collect { currentPage ->  onPage(currentPage) }
    }
}
```

6. Заменить анимации на suspend-функции - особенно важно, когда результат анимации передается вглубь дочерних composable-функций:

```
@Composable
fun AnimateSomething(something: State<Int>) {
    val animatedValue = remember { Animatable(initialValue = 0f) }

    LaunchedEffect(Unit) {
        snapshotFlow { something.value } // вне рекомпозиции
            .collect { newValue ->
                animatedValue.animateTo(newValue, tween(DURATION)) // анимация вне рекомпозиции
            }
    }

    Canvas { 
        drawSomething(animatedValue.value) // анимация читается в самой дочерней функции, не задевая родительские, читаем и анимируем вне рекомпозиции на стадии прорисовки
    }
}
```

7. Архитектура State-моделей экрана
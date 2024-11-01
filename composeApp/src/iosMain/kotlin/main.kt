import androidx.compose.ui.window.ComposeUIViewController
import io.github.dmitriy1892.kmptipsandtricks.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }

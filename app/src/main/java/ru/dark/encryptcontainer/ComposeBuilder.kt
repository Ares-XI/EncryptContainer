package ru.dark.encryptcontainer

import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson

abstract class ComposeBuilder {
    protected val gson = Gson()
    protected val uiWidth = 350.dp
    protected val uiHeight = 200.dp
    protected val buttonHeight = 48.dp
    protected val buttonColor = ButtonColors(Color.LightGray, Color.Black, Color.LightGray, Color.Black)
    protected val buttonTextColor = Color.Black

    @Composable
    abstract fun Compose(navController: NavController)

    protected fun compareStrings(str1: String, str2: String): Boolean {
        if (str1 == str2) return false
        if (str1.length < str2.length && str2.startsWith(str1)) return true
        return false
    }
}
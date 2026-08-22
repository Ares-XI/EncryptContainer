package ru.dark.encryptcontainer.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ru.dark.encryptcontainer.ComposeBuilder
import ru.dark.encryptcontainer.MainActivity

object MainScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val context = LocalContext.current

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.ENCRYPT_SCREEN)
            }) {
                Text("Зашифровать", color = buttonTextColor, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.DECRYPT_SCREEN)
            }) {
                Text("Расшифровать", color = buttonTextColor, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.KEYS_SCREEN)
            }) {
                Text("Данные ключей", color = buttonTextColor, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.MESSAGES_SCREEN)
            }) {
                Text("Данные сообщений", color = buttonTextColor, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.EXPORT_SCREEN)
            }) {
                Text("Экспорт данных", color = buttonTextColor, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                navController.navigate(MainActivity.IMPORT_SCREEN)
            }) {
                Text("Импорт данных", color = buttonTextColor, fontSize = 24.sp)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Button(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                (context as? Activity)?.finishAffinity()
            }) {
                Text("Выйти из приложения", color = buttonTextColor, fontSize = 24.sp)
            }
        }
    }
}
package ru.dark.encryptcontainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import ru.dark.encryptcontainer.crypto.CryptoManager
import ru.dark.encryptcontainer.ui.DecryptScreen
import ru.dark.encryptcontainer.ui.EncryptScreen
import ru.dark.encryptcontainer.ui.ExportScreen
import ru.dark.encryptcontainer.ui.ImportScreen
import ru.dark.encryptcontainer.ui.KeysScreen
import ru.dark.encryptcontainer.ui.MainScreen
import ru.dark.encryptcontainer.ui.MessagesScreen

class MainActivity : ComponentActivity() {
    companion object {
        const val IS_EXPORT_ENABLED = true //Маркер (разрешающий/запрещающий) экспортировать базу данных.

        const val MAIN_SCREEN = "main_screen"
        const val ENCRYPT_SCREEN = "encrypt_screen"
        const val DECRYPT_SCREEN = "decrypt_screen"
        const val KEYS_SCREEN = "keys_screen"
        const val MESSAGES_SCREEN = "messages_screen"
        const val EXPORT_SCREEN = "export_screen"
        const val IMPORT_SCREEN = "import_screen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CryptoManager.init(this)
        setContent {
             MaterialTheme {
                val navController = rememberNavController()

                NavHost(navController, MAIN_SCREEN) {
                    composable(MAIN_SCREEN) { MainScreen.Compose(navController) }
                    composable(ENCRYPT_SCREEN) { EncryptScreen.Compose(navController) }
                    composable(DECRYPT_SCREEN) { DecryptScreen.Compose(navController) }
                    composable(KEYS_SCREEN) { KeysScreen.Compose(navController) }
                    composable(MESSAGES_SCREEN) { MessagesScreen.Compose(navController) }
                    composable(EXPORT_SCREEN) { ExportScreen.Compose(navController) }
                    composable(IMPORT_SCREEN) { ImportScreen.Compose(navController) }
                }
            }
        }
    }
}
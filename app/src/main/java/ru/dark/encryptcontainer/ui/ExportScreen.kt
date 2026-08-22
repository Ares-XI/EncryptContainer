package ru.dark.encryptcontainer.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.dark.encryptcontainer.ComposeBuilder
import ru.dark.encryptcontainer.MainActivity
import ru.dark.encryptcontainer.crypto.CryptoManager
import ru.dark.encryptcontainer.crypto.CryptoUtils
import ru.dark.encryptcontainer.crypto.json.DataBase
import ru.dark.encryptcontainer.crypto.json.EncryptedDataBase
import ru.dark.encryptcontainer.crypto.json.KeyCache
import ru.dark.encryptcontainer.crypto.json.MessageCache

object ExportScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val lifecycleScope = rememberCoroutineScope()
        val passwordScrollState = rememberScrollState()

        var isBlocked by remember { mutableStateOf(false) }
        var lastPassword by remember { mutableStateOf("") }
        var passwordFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var outputTextValue by remember { mutableStateOf(TextFieldValue("")) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = passwordFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(passwordScrollState),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        passwordFieldValue = it
                        if(compareStrings(lastPassword, passwordFieldValue.text)) lifecycleScope.launch { passwordScrollState.scrollTo(passwordScrollState.maxValue) }
                        lastPassword = passwordFieldValue.text
                    },
                    label = {
                        Text("Пароль:", color = Color.Gray, fontSize = 16.sp)
                    },
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = outputTextValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(rememberScrollState()),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        outputTextValue = it
                    }, label = {
                        Text("Экспортированная база данных:", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onExportButtonClicked(lifecycleScope, passwordFieldValue.text, onResult = { output ->
                                outputTextValue = TextFieldValue(output)
                                isBlocked = false
                            }, onFailed = {
                                Toast.makeText(navController.context, "Ошибка экспорта, подробнее в логе...", Toast.LENGTH_SHORT).show()
                                isBlocked = false
                            }, onUnsupported = {
                                Toast.makeText(navController.context, "Экспорт недоступен", Toast.LENGTH_SHORT).show()
                                isBlocked = false
                            })
                        }
                    }
                ) {
                    if(!isBlocked) Text("Экспорт", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Экспортирую...", color = buttonTextColor, fontSize = 24.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                    if(!isBlocked) navController.navigate(MainActivity.MAIN_SCREEN)
                    else Toast.makeText(navController.context, "Экспортирую, подождите...", Toast.LENGTH_SHORT).show()
                }) {
                Text("Назад", color = buttonTextColor, fontSize = 24.sp)
            }
        }
    }

    private fun onExportButtonClicked(coroutine: CoroutineScope, password: String, onResult: (String) -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(MainActivity.IS_EXPORT_ENABLED) {
            coroutine.launch {
                withContext(Dispatchers.IO) {
                    try {
                        var output = ""
                        val database = DataBase(
                            CryptoManager.getCurrentKeyPair(),
                            KeyCache().apply {
                                cache = CryptoManager.getRetiredKeyPairs().toMutableSet()
                            },
                            MessageCache().apply {
                                cache = CryptoManager.getEncryptToMessagePairs().toMutableSet()
                            })
                        output = gson.toJson(database)
                        if (password.isNotEmpty()) output = gson.toJson(EncryptedDataBase().apply {
                            encryptedJson = CryptoUtils.encryptFile(output, password)
                        })
                        withContext(Dispatchers.Main) {
                            onResult(output)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            onFailed()
                        }
                    }
                }
            }
        } else {
            onUnsupported()
        }
    }
}
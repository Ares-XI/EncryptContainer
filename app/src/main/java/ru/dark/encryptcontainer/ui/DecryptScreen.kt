package ru.dark.encryptcontainer.ui

import android.widget.Toast
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import ru.dark.encryptcontainer.crypto.json.EncryptOutput

object DecryptScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val lifecycleScope = rememberCoroutineScope()
        val messageScrollState = rememberScrollState()

        var isBlocked by remember { mutableStateOf(false) }

        var messageFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var outputFieldValue by remember { mutableStateOf(TextFieldValue("")) }

        var lastMessageText by remember { mutableStateOf("") }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = messageFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(messageScrollState),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        messageFieldValue = it
                        if (compareStrings(lastMessageText, messageFieldValue.text)) lifecycleScope.launch { messageScrollState.scrollTo(messageScrollState.maxValue) }
                        lastMessageText = messageFieldValue.text
                    },
                    label = {
                        Text("Сообщение:", color = Color.Gray, fontSize = 16.sp)
                    },
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = outputFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(rememberScrollState()),
                    singleLine = false,
                    readOnly = true,
                    onValueChange = {},
                    colors = OutlinedTextFieldDefaults.colors(),
                    label = {
                        Text("Расшифровка:", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onDecryptButtonClicked(lifecycleScope, messageFieldValue.text, onFailed = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Ошибка расшифровывания! Подробнее в логе...", Toast.LENGTH_SHORT).show()
                            }, onUnsupported = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Заполните оба поля", Toast.LENGTH_SHORT).show()
                            }, onResult =  { output ->
                                outputFieldValue = TextFieldValue(output)
                                isBlocked = false
                            })
                        }
                    }
                ) {
                    if(!isBlocked) Text("Расшифровать", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Расшифровываю...", color = buttonTextColor, fontSize = 24.sp)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onDecryptFullButtonClicked(lifecycleScope, messageFieldValue.text, onFailed = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Ошибка расшифровывания! Подробнее в логе...", Toast.LENGTH_SHORT).show()
                            }, onUnsupported = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Заполните оба поля", Toast.LENGTH_SHORT).show()
                            }, onResult =  { output ->
                                outputFieldValue = TextFieldValue(output)
                                isBlocked = false
                            })
                        }
                    }
                ) {
                    if(!isBlocked) Text("Расшифровать, включая устаревшие ключи", color = buttonTextColor, fontSize = 14.sp)
                    else Text("Расшифровываю", color = buttonTextColor, fontSize = 16.sp)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Button(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                if(!isBlocked) navController.navigate(MainActivity.MAIN_SCREEN)
                else Toast.makeText(navController.context, "Текст расшифровывается, подождите...", Toast.LENGTH_SHORT).show()
            }) {
                Text("Назад", color = buttonTextColor, fontSize = 24.sp)
            }
        }
    }

    private fun onDecryptButtonClicked(coroutine: CoroutineScope, input: String, onResult: (String) -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(input.isEmpty()) {
            onUnsupported()
            return
        }
        coroutine.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = gson.fromJson(input, EncryptOutput::class.java)
                    val message = CryptoUtils.decrypt(CryptoManager.getCurrentKeyPair()!!.second, output.encryptedAesKey, output.encryptedAesValue)
                    withContext(Dispatchers.Main) {
                        onResult(message)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onFailed() }
            }
        }
    }

    private fun onDecryptFullButtonClicked(coroutine: CoroutineScope, input: String, onResult: (String) -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(input.isEmpty()) {
            onUnsupported()
            return
        }
        coroutine.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = gson.fromJson(input, EncryptOutput::class.java)
                    try {
                        val messageBefore = CryptoUtils.decrypt(CryptoManager.getCurrentKeyPair()!!.second, output.encryptedAesKey, output.encryptedAesValue)
                        withContext(Dispatchers.Main) {
                            onResult(messageBefore)
                        }
                    } catch (_: Throwable) {
                        CryptoManager.getRetiredKeyPairs().forEach { pair ->
                            try {
                                val message = CryptoUtils.decrypt(pair.second.replace(" ", "").replace("\n", ""), output.encryptedAesKey, output.encryptedAesValue)
                                withContext(Dispatchers.Main) {
                                    onResult(message)
                                }
                                return@withContext
                            } catch (_: Throwable) {
                                return@forEach
                            }
                        }
                        withContext(Dispatchers.Main) {
                            onFailed()
                        }
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onFailed()
                }
            }
        }
    }
}
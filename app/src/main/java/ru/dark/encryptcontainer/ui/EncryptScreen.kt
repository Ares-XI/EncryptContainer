package ru.dark.encryptcontainer.ui

import android.widget.Toast
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import kotlin.apply

object EncryptScreen: ComposeBuilder() {
    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Compose(navController: NavController) {
        val clipboardManager = LocalClipboardManager.current

        val messageScrollState = rememberScrollState()
        val keyScrollState = rememberScrollState()
        val lifecycleScope = rememberCoroutineScope()

        var isBlocked by remember { mutableStateOf(false) }

        var messageFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var keyFieldValue by remember { mutableStateOf(TextFieldValue("")) }

        var lastMessageText by remember { mutableStateOf("") }
        var lastKeyText by remember { mutableStateOf("") }

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
                        if(compareStrings(lastMessageText, messageFieldValue.text)) lifecycleScope.launch { messageScrollState.scrollTo(messageScrollState.maxValue) }
                        lastMessageText = messageFieldValue.text
                    }, label = {
                        Text("Сообщение:", color = Color.Gray, fontSize = 16.sp)
                    },
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = keyFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(keyScrollState),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        keyFieldValue = it
                        if(compareStrings(lastKeyText, keyFieldValue.text)) lifecycleScope.launch { keyScrollState.scrollTo(keyScrollState.maxValue) }
                        lastKeyText = keyFieldValue.text
                    }, label = {
                        Text("Ключ:", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onEncryptButtonClicked(lifecycleScope, messageFieldValue.text, keyFieldValue.text, onFailed = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Ошибка засшифровывания! Подробнее в логе...", Toast.LENGTH_SHORT).show()
                            }, onUnsupported = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Заполните оба поля", Toast.LENGTH_SHORT).show()
                            }, onResult =  { output ->
                                try {
                                    clipboardManager.setText(AnnotatedString(gson.toJson(EncryptOutput().apply {
                                        encryptedAesKey = output.first
                                        encryptedAesValue = output.second
                                    })))
                                    Toast.makeText(navController.context, "Зашифровано! Текст скопирован.", Toast.LENGTH_SHORT).show()
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                    Toast.makeText(navController.context, "Ошибка засшифровывания! Подробнее в логе...", Toast.LENGTH_SHORT).show()
                                }
                                isBlocked = false
                            })
                        }
                    }
                ) {
                    if(!isBlocked) Text("Зашифровать", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Зашифровываю...", color = buttonTextColor, fontSize = 24.sp)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Button(modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                    if(!isBlocked) navController.navigate(MainActivity.MAIN_SCREEN)
                    else Toast.makeText(navController.context, "Текст зашифровывается, подождите...", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Назад", color = buttonTextColor, fontSize = 24.sp)
                }
            }
        }
    }

    private fun onEncryptButtonClicked(coroutine: CoroutineScope, message: String, key: String, onResult: (Pair<String, String>) -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(message.isEmpty() || key.isEmpty()) {
            onUnsupported()
            return
        }
        coroutine.launch {
            try {
                withContext(Dispatchers.IO) {
                    val encrypted = CryptoUtils.encrypt(key.replace(" ", "").replace("\n", ""), message)
                    val json = gson.toJson(EncryptOutput().apply {
                        encryptedAesKey = encrypted.first
                        encryptedAesValue = encrypted.second
                    })
                    CryptoManager.addEncryptToMessagePair(Pair(json, message))
                    withContext(Dispatchers.Main) {
                        onResult(encrypted)
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
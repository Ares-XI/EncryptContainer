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
import ru.dark.encryptcontainer.crypto.json.EncryptOutput

object MessagesScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val lifecycleScope = rememberCoroutineScope()
        val scrollState = rememberScrollState()

        var isBlocked by remember { mutableStateOf(false) }
        var lastEncryptedText by remember { mutableStateOf("") }
        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var outputTextValue by remember { mutableStateOf(TextFieldValue("")) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(scrollState),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        textFieldValue = it
                        if(compareStrings(lastEncryptedText, textFieldValue.text)) lifecycleScope.launch { scrollState.scrollTo(scrollState.maxValue) }
                        lastEncryptedText = textFieldValue.text
                    },
                    label = {
                        Text("Зашифрованное сообщение:", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = outputTextValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(rememberScrollState()),
                    singleLine = false,
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {},
                    label = {
                        Text("Твоё отправленное сообщение:", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth), colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onGetTextClickButton(lifecycleScope, textFieldValue.text,
                                onResult = { result ->
                                    outputTextValue = TextFieldValue(result)
                                    isBlocked = false
                                },
                                onUnsupported = {
                                    isBlocked = false
                                    Toast.makeText(navController.context, "Заполните поле c вводом текста", Toast.LENGTH_SHORT).show()
                                },
                                onFailed = {
                                    isBlocked = false
                                    Toast.makeText(navController.context, "Не удалось найти текст, подробнее в логе...", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    if(!isBlocked) Text("Получить сообщение", color = buttonTextColor, fontSize = 20.sp)
                    else Text("Получаю...", color = buttonTextColor, fontSize = 20.sp)
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                    if(!isBlocked) navController.navigate(MainActivity.MAIN_SCREEN)
                    else Toast.makeText(navController.context, "Получаю текст, подождите...", Toast.LENGTH_SHORT).show()
                }) {
                Text("Назад", color = buttonTextColor, fontSize = 24.sp)
            }
        }
    }

    private fun onGetTextClickButton(coroutine: CoroutineScope, text: String, onResult: (String) -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(text.isEmpty()) {
            onUnsupported()
            return
        }
        coroutine.launch {
            try {
                withContext(Dispatchers.IO) {
                    val output = gson.fromJson(text, EncryptOutput::class.java)
                    CryptoManager.getEncryptToMessagePairs().forEach { pair ->
                        try {
                            if(gson.fromJson(pair.first, EncryptOutput::class.java).equals(output)) {
                                withContext(Dispatchers.Main) { onResult(pair.second) }
                                return@withContext
                            }
                        } catch (_: Throwable) {
                            return@forEach
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onFailed()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onFailed() }
            }
        }
    }
}
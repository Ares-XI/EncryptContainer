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

object KeysScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val clipboardManager = LocalClipboardManager.current

        val lifecycleScope = rememberCoroutineScope()

        var isBlocked by remember { mutableStateOf(false) }
        var rsaAllPairsFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var rsaNewPairFieldValue by remember { mutableStateOf(TextFieldValue("")) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = rsaAllPairsFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiHeight).verticalScroll(rememberScrollState()),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        rsaAllPairsFieldValue = it
                    },
                    label = {
                        Text("Публичные ключи: ", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            val currentPair = CryptoManager.getCurrentKeyPair()
                            val retiredPairs = CryptoManager.getRetiredKeyPairs()
                            var text = "Текущая пара:\n"
                            text += if(currentPair == null) "- <отсутствует>\n" else "- Публичный ключ: \n-- ${currentPair.first}\n"
                            text += "Устаревшие пары:\n"
                            if(retiredPairs.isEmpty()) text += "- <отсутствуют>"
                            retiredPairs.forEach { pair -> text += "- Пара:\n-- ${pair.first}\n" }
                            rsaAllPairsFieldValue = TextFieldValue(text)
                            isBlocked = false
                        }
                    }
                ) {
                    if(!isBlocked) Text("Получить ключи", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Получаю...", color = buttonTextColor, fontSize = 24.sp)
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = rsaNewPairFieldValue,
                    modifier = Modifier.width(350.dp).height(96.dp).verticalScroll(rememberScrollState()),
                    singleLine = false,
                    readOnly = true,
                    onValueChange = {},
                    colors = OutlinedTextFieldDefaults.colors(),
                    label = {
                        Text("Новая пара ключей: ", color = Color.Gray, fontSize = 16.sp)
                    }
                )

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onGenerateNewRSAPairButtonClicked(lifecycleScope) { pair ->
                                rsaNewPairFieldValue = TextFieldValue("Текущая пара:\n- Публичный ключ:\n-- ${pair.first}")
                                isBlocked = false
                            }
                        }
                    }
                ) {
                    if(!isBlocked) Text("Новая пара ключей", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Генерирую...", color = buttonTextColor, fontSize = 24.sp)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            lifecycleScope.launch {
                                withContext(Dispatchers.IO) {
                                    val key = CryptoManager.getCurrentKeyPair()
                                    if(key == null) {
                                        Toast.makeText(navController.context, "Текущий публичный ключ отсутсвует, сгенерируйте новый", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(navController.context, "Текущий публичный ключ скопирован!", Toast.LENGTH_SHORT).show()
                                    }
                                    clipboardManager.setText(AnnotatedString(key.first))
                                }
                            }
                            isBlocked = false
                        }
                    }
                ) {
                    if(!isBlocked) Text("Копировать публ. ключ", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Копирую...", color = buttonTextColor, fontSize = 24.sp)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Button(
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-64).dp).height(buttonHeight).width(uiWidth), colors = buttonColor, onClick = {
                    if(!isBlocked) navController.navigate(MainActivity.MAIN_SCREEN)
                    else Toast.makeText(navController.context, "Текст зашифровывается, подождите...", Toast.LENGTH_SHORT).show()
                }) {
                Text("Назад", color = buttonTextColor, fontSize = 24.sp)
            }
        }
    }

    private fun onGenerateNewRSAPairButtonClicked(coroutine: CoroutineScope, onResult: (Pair<String, String>) -> Unit) {
        coroutine.launch {
            withContext(Dispatchers.IO) {
                val pair = CryptoUtils.generateKeyPair()
                CryptoManager.setCurrentKeyPair(pair.first, pair.second)
                withContext(Dispatchers.Main) {
                    onResult(pair)
                }
            }
        }
    }
}
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
import ru.dark.encryptcontainer.crypto.CryptoUtils
import ru.dark.encryptcontainer.crypto.json.DataBase
import ru.dark.encryptcontainer.crypto.json.EncryptedDataBase

object ImportScreen: ComposeBuilder() {
    @Composable
    override fun Compose(navController: NavController) {
        val lifecycleScope = rememberCoroutineScope()
        val databaseScrollState = rememberScrollState()
        val passwordScrollState = rememberScrollState()

        var isBlocked by remember { mutableStateOf(false) }

        var lastPassword by remember { mutableStateOf("") }
        var lastDatabase by remember { mutableStateOf("") }

        var passwordFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var databaseFieldValue by remember { mutableStateOf(TextFieldValue("")) }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top, horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(
                    value = databaseFieldValue,
                    modifier = Modifier.width(uiWidth).height(uiWidth).verticalScroll(databaseScrollState),
                    singleLine = false,
                    colors = OutlinedTextFieldDefaults.colors(),
                    onValueChange = {
                        databaseFieldValue = it
                        if(compareStrings(lastDatabase, databaseFieldValue.text)) lifecycleScope.launch { databaseScrollState.scrollTo(databaseScrollState.maxValue) }
                        lastDatabase = databaseFieldValue.text
                    },
                    label = {
                        Text("База данных:", color = Color.Gray, fontSize = 16.sp)
                    },
                )

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = passwordFieldValue,
                    modifier = Modifier.width(uiWidth).height(96.dp).verticalScroll(passwordScrollState),
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

                Button(
                    modifier = Modifier.height(buttonHeight).width(uiWidth),
                    colors = buttonColor,
                    onClick = {
                        if(!isBlocked) {
                            isBlocked = true
                            onImportButtonClicked(lifecycleScope,databaseFieldValue.text, passwordFieldValue.text, onResult = {
                                Toast.makeText(navController.context, "База данных успешно испортирована", Toast.LENGTH_SHORT).show()
                                isBlocked = false
                            }, onFailed = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Ошибка импорта, подробнее в логе...", Toast.LENGTH_SHORT).show()
                            }, onUnsupported = {
                                isBlocked = false
                                Toast.makeText(navController.context, "Заполните поле c вводом текста", Toast.LENGTH_SHORT).show()
                            })
                        }
                    }
                ) {
                    if(!isBlocked) Text("Импорт", color = buttonTextColor, fontSize = 24.sp)
                    else Text("Импортирую...", color = buttonTextColor, fontSize = 24.sp)
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

    private fun onImportButtonClicked(coroutine: CoroutineScope, dataBase: String, password: String, onResult: () -> Unit, onFailed: () -> Unit, onUnsupported: () -> Unit) {
        if(dataBase.isEmpty()) {
            onUnsupported()
            return
        }
        coroutine.launch {
            withContext(Dispatchers.IO) {
                var json: DataBase? = null
                try {
                    json = gson.fromJson(dataBase, DataBase::class.java)
                    importUtil(json)
                    withContext(Dispatchers.Main) {
                        onResult()
                    }
                    return@withContext
                } catch (_: Throwable) {
                    try {
                        json = gson.fromJson(CryptoUtils.decryptFile(gson.fromJson(dataBase, EncryptedDataBase::class.java).encryptedJson, password), DataBase::class.java)
                        importUtil(json)
                        withContext(Dispatchers.Main) {
                            onResult()
                        }
                        return@withContext
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            onFailed()
                            return@withContext
                        }
                    }
                }
            }
        }
    }

    private fun importUtil(base: DataBase) {
        if(base.currentKeyPair != null && CryptoManager.getCurrentKeyPair() == null) CryptoManager.setCurrentKeyPair(base.currentKeyPair.first, base.currentKeyPair.second)
        else if(base.currentKeyPair != null && CryptoManager.getCurrentKeyPair() != null) CryptoManager.addRetiredKeyPair(base.currentKeyPair)
        base.retiredKeyCache.cache.forEach { pair ->
            if(!CryptoManager.getRetiredKeyPairs().contains(pair)) {
                CryptoManager.addRetiredKeyPair(pair)
            }
        }
        base.messageCache.cache.forEach { pair ->
            if(!CryptoManager.getEncryptToMessagePairs().contains(pair)) {
                CryptoManager.addEncryptToMessagePair(pair)
            }
        }
    }
}
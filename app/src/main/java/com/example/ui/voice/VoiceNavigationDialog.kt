package com.example.ui.voice

import com.example.ui.settings.L
import com.example.ui.settings.Lf

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.settings.LocalAppSettings
import com.example.ui.settings.tr
import java.util.Locale

@Composable
fun VoiceNavigationDialog(
    onCommand: (String, VoiceCommandResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val language = LocalAppSettings.current.language
    val initialStatus = tr("დააჭირეთ მიკროფონს და თქვით ბრძანება", "Нажмите микрофон и произнесите команду")
    var transcript by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    var status by remember(language) { mutableStateOf(initialStatus) }
    val tts = remember { TextToSpeech(context) {} }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.shutdown() } }

    val recognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) { listening = true }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { listening = false }
                override fun onError(error: Int) { listening = false; status = L("ვერ გავიგე, სცადეთ კიდევ") }
                override fun onResults(results: android.os.Bundle?) {
                    listening = false
                    transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (transcript.isNotBlank()) {
                        val command = VoiceCommandParser.parse(transcript)
                        status = if (command.screen == null && command.search != null) com.example.ui.settings.Lf("ვიწყებ ძებნას: {0}", command.search) else L(command.messageKa)
                        val locale = Locale.forLanguageTag(language.tag)
                        tts.language = locale
                        tts.speak(status, TextToSpeech.QUEUE_FLUSH, null, "voice_result")
                        onCommand(transcript, command)
                    }
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    transcript = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                }
                override fun onEvent(eventType: Int, params: android.os.Bundle?) = Unit
            })
        }
    }
    DisposableEffect(recognizer) { onDispose { recognizer.destroy() } }

    fun start() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            start()
        } else {
            status = L("მიკროფონის ნებართვა საჭიროა")
        }
    }

    AlertDialog(
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.94f),
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(tr("ხმოვანი მართვა", "Голосовое управление"), style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, tr("დახურვა", "Закрыть")) }
            }
        },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(status, style = MaterialTheme.typography.bodyMedium)
                FilledIconButton(
                    onClick = {
                        if (listening) { recognizer.stopListening(); listening = false }
                        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.size(72.dp)
                ) { Icon(if (listening) Icons.Default.Stop else Icons.Default.Mic, null, Modifier.size(34.dp)) }
                if (transcript.isNotBlank()) Text("“$transcript”", style = MaterialTheme.typography.bodyLarge)
                Text(tr("მაგალითი: „გახსენი ფინანსები“, „მოძებნე დავით“, „დაამატე წევრი“", "Пример: «открой финансы», «найди Давида», «добавь участника»"), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {}
    )
}

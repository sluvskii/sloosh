package com.slooshfilm.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.slooshfilm.app.utils.CrashLogs

/**
 * Диалог логов падения.
 *
 * Два сценария:
 * 1. **Автоматически на запуске** — передаётся [report] (непросмотренный отчёт новой сборки).
 *    Показывается один раз, окно не навязчивое: сразу видно, что можно закрыть.
 * 2. **Вручную из настроек** — вызывается без [report]: показывает сводку всех сохранённых
 *    отчётов, с переключением между ними и кнопкой отправки через системное «Поделиться».
 *
 * Стекло здесь намеренно не используется: диалог может появиться до готовности стеклянного
 * движка и не должен зависеть от того, что именно сломалось.
 */
@Composable
fun CrashReportDialog(
    onDismiss: () -> Unit,
    report: CrashLogs.Report? = null,
    title: String = if (report != null) "Отчёт о падении" else "Логи падений",
    showSummaryWhenEmpty: Boolean = report == null
) {
    val context = LocalContext.current
    val reports = remember { CrashLogs.all(context) }
    // -1 — сводка, иначе индекс открытого отчёта. При автоматическом показе открыт сразу он.
    val initialIndex = remember {
        if (report == null) -1 else reports.indexOfFirst { it.fileName == report.fileName }.coerceAtLeast(0)
    }
    var index by remember { mutableIntStateOf(initialIndex) }
    val openReport = reports.getOrNull(index)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (openReport != null) {
                    "Скопируйте текст или отправьте его разработчику."
                } else {
                    "Сохранённых отчётов: ${reports.size}. Выберите нужный."
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (openReport != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(max = 260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .verticalScroll(rememberScrollState())
                        .padding(10.dp)
                ) {
                    Text(
                        text = openReport.text,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else if (reports.isEmpty()) {
                Text(
                    text = "Падений не зафиксировано.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reports.forEachIndexed { i, item ->
                        CrashReportRow(
                            text = item.fileName.removePrefix("crash_").removeSuffix(".txt").replace('_', ' '),
                            onClick = { index = i }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (openReport != null) {
                    if (reports.size > 1) {
                        CrashReportAction("Все логи", Modifier.weight(1f)) { index = -1 }
                    }
                    CrashReportAction("Копировать", Modifier.weight(1f)) {
                        copyToClipboard(context, openReport.text)
                    }
                    CrashReportAction("Отправить", Modifier.weight(1f)) {
                        shareReport(context, openReport.text)
                    }
                } else {
                    if (reports.isNotEmpty()) {
                        CrashReportAction("Отправить все", Modifier.weight(1f)) {
                            shareReport(context, reports.joinToString("\n\n") { it.text })
                        }
                        CrashReportAction("Очистить", Modifier.weight(1f)) {
                            CrashLogs.clear(context)
                            onDismiss()
                        }
                    }
                    CrashReportAction("Закрыть", Modifier.weight(1f), onClick = onDismiss)
                }
            }

            if (openReport != null) {
                CrashReportAction(
                    text = "Закрыть",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = onDismiss
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("sloosh crash", text))
    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Логи sloosh")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Отправить логи")) }
}

@Composable
private fun CrashReportRow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Плоская капсула действия внутри диалога логов. */
@Composable
private fun CrashReportAction(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

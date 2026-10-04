package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.settings.Currencies
import com.example.ui.settings.L
import com.example.ui.settings.Lf
import com.example.ui.settings.nb
import com.example.ui.theme.LocalGabbaiPalette

/* ------------------------------------------------------------------------------------------
 * All pickers open a separate full-width dialog with a search box and a bounded list.
 * (The old in-place menu put a lazy list inside a menu that measures intrinsic sizes,
 * which crashed the app as soon as a dropdown was tapped.)
 * ---------------------------------------------------------------------------------------- */

/** The raised 3D "glass" field that shows the current value and opens a picker. */
@Composable
fun DropdownFace(
    label: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
    trailingIcon: ImageVector = Icons.Default.KeyboardArrowDown,
    compact: Boolean = false
) {
    val palette = LocalGabbaiPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(if (compact) 14.dp else 18.dp)
    Column(modifier) {
        if (label.isNotEmpty()) {
            Text(
                text = nb(label),
                color = palette.secondaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
            )
        }
        // base lip (gives the 3D thickness)
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(12.dp, shape, ambientColor = primary.copy(alpha = 0.35f), spotColor = primary.copy(alpha = 0.5f))
                .clip(shape)
                .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.55f), primary.copy(alpha = 0.95f))))
                .padding(bottom = 4.dp)
                .clickable(onClick = onClick)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(palette.glass, palette.surface, palette.background)))
                    .border(1.5.dp, Brush.verticalGradient(listOf(primary.copy(alpha = 0.25f), primary.copy(alpha = 0.9f))), shape)
            ) {
                // top light reflection
                Box(
                    Modifier.fillMaxWidth().height(16.dp).padding(horizontal = 10.dp, vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (palette.isNight) 0.14f else 0.45f), Color.Transparent)))
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = if (compact) 48.dp else 54.dp).padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = nb(text),
                        color = if (placeholder) palette.mutedText else palette.text,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.size(if (compact) 32.dp else 38.dp)
                            .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = primary)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.8f), primary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(trailingIcon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

/** A row shown at the top of the picker list (e.g. "use typed text", "add to members"). */
data class PickerAction(val label: String, val icon: ImageVector, val onClick: (query: String) -> Unit)

/** Searchable picker dialog. Every list in the app goes through here, so every list is filterable. */
@Composable
fun <T> SearchPickerDialog(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    selected: T? = null,
    initialQuery: String = "",
    actions: (query: String) -> List<PickerAction> = { emptyList() }
) {
    val palette = LocalGabbaiPalette.current
    val primary = MaterialTheme.colorScheme.primary
    var query by remember { mutableStateOf(initialQuery) }
    val focus = remember { FocusRequester() }
    val visible = remember(query, options) {
        val q = query.trim()
        if (q.isEmpty()) options else options.filter { optionLabel(it).contains(q, ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = palette.surface,
            shadowElevation = 24.dp,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), primary.copy(alpha = 0.7f)))),
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 600.dp).testTag("picker_dialog")
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nb(title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = palette.text, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, L("დახურვა"), tint = palette.secondaryText) }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(L("ძიება ან აკრეფა…"), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = primary) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, null) } },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("picker_search")
                )
                Spacer(Modifier.height(10.dp))
                val extra = actions(query.trim())
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(extra) { _, a ->
                        PickerRow(text = a.label, icon = a.icon, highlighted = true, onClick = { a.onClick(query.trim()) })
                    }
                    itemsIndexed(visible) { _, item ->
                        PickerRow(
                            text = optionLabel(item),
                            icon = if (item == selected) Icons.Default.CheckCircle else null,
                            highlighted = item == selected,
                            onClick = { onPick(item) }
                        )
                    }
                    if (visible.isEmpty() && extra.isEmpty()) {
                        itemsIndexed(listOf(0)) { _, _ ->
                            Text(L("ვერაფერი მოიძებნა"), color = palette.mutedText, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(text: String, icon: ImageVector?, highlighted: Boolean, onClick: () -> Unit) {
    val palette = LocalGabbaiPalette.current
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (highlighted) primary.copy(alpha = 0.16f) else palette.glass)
            .border(1.dp, if (highlighted) primary.copy(alpha = 0.7f) else palette.border.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .heightIn(min = 50.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(nb(text), color = palette.text, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** Fixed-list dropdown with search. Same call signature as before so every screen keeps working. */
@Composable
fun <T> AppDropdown(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    displayText: String? = null
) {
    var open by remember { mutableStateOf(false) }
    DropdownFace(label = label, text = displayText ?: optionLabel(value), onClick = { open = true }, modifier = modifier.fillMaxWidth())
    if (open) {
        SearchPickerDialog(
            title = label,
            options = options,
            optionLabel = optionLabel,
            selected = if (displayText == null) value else options.firstOrNull { optionLabel(it) == displayText },
            onPick = { onSelected(it); open = false },
            onDismiss = { open = false }
        )
    }
}

/** Dropdown that also accepts free text: pick from the list or type something new in the same field. */
@Composable
fun SuggestField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    modifier: Modifier = Modifier,
    placeholder: String = L("აირჩიეთ ან აკრიფეთ"),
    onSuggestionPicked: (String) -> Unit = {}
) {
    var open by remember { mutableStateOf(false) }
    DropdownFace(
        label = label,
        text = value.ifEmpty { placeholder },
        placeholder = value.isEmpty(),
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = Icons.Default.EditNote
    )
    if (open) {
        SearchPickerDialog(
            title = label,
            options = suggestions,
            optionLabel = { it },
            selected = value.takeIf { it in suggestions },
            initialQuery = "",
            onPick = { onValueChange(it); onSuggestionPicked(it); open = false },
            onDismiss = { open = false },
            actions = { q ->
                if (q.isNotEmpty() && suggestions.none { it.equals(q, ignoreCase = true) })
                    listOf(PickerAction(Lf("გამოყენება: «{0}»", q), Icons.Default.Edit) { onValueChange(it); open = false })
                else emptyList()
            }
        )
    }
}

/**
 * Person field bound to the members database: filters as you type; if the name is not found,
 * offers to add it to the members list (with confirmation) or just use it once.
 */
@Composable
fun MemberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    memberNames: List<String>,
    onAddMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    var confirmAdd by remember { mutableStateOf<String?>(null) }
    DropdownFace(
        label = label,
        text = value.ifEmpty { L("აირჩიეთ წევრი ან აკრიფეთ სახელი") },
        placeholder = value.isEmpty(),
        onClick = { open = true },
        modifier = modifier.fillMaxWidth(),
        trailingIcon = Icons.Default.PersonSearch
    )
    if (open) {
        SearchPickerDialog(
            title = label,
            options = memberNames.distinct().sorted(),
            optionLabel = { it },
            selected = value.takeIf { it.isNotEmpty() },
            onPick = { onValueChange(it); open = false },
            onDismiss = { open = false },
            actions = { q ->
                if (q.isNotEmpty() && memberNames.none { it.equals(q, ignoreCase = true) }) listOf(
                    PickerAction(Lf("ბაზაში დამატება: «{0}»", q), Icons.Default.PersonAdd) { confirmAdd = it },
                    PickerAction(Lf("მხოლოდ ერთჯერადად: «{0}»", q), Icons.Default.Edit) { onValueChange(it); open = false }
                ) else emptyList()
            }
        )
    }
    confirmAdd?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmAdd = null },
            icon = { Icon(Icons.Default.PersonAdd, null) },
            title = { Text(L("ახალი წევრის დამატება")) },
            text = { Text(Lf("«{0}» არ არის წევრების ბაზაში. დავამატო?", name)) },
            confirmButton = {
                Button(onClick = { onAddMember(name); onValueChange(name); confirmAdd = null; open = false }) { Text(L("დიახ, დამატება")) }
            },
            dismissButton = {
                TextButton(onClick = { onValueChange(name); confirmAdd = null; open = false }) { Text(L("არა, მხოლოდ ჩაწერა")) }
            }
        )
    }
}

/** Amount input with a currency selector next to it. */
@Composable
fun AmountCurrencyField(
    label: String,
    amountText: String,
    onAmountChange: (String) -> Unit,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { t -> onAmountChange(t.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')) },
            label = { Text(nb(label), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        val c = Currencies.find(currency)
        DropdownFace(
            label = "",
            text = "${c.symbol} ${c.code}",
            onClick = { open = true },
            compact = true,
            modifier = Modifier.width(118.dp)
        )
    }
    if (open) {
        SearchPickerDialog(
            title = L("ვალუტა"),
            options = Currencies.all,
            optionLabel = { "${it.symbol}  ${it.code} — ${L(it.nameKa)}" },
            selected = Currencies.find(currency),
            onPick = { onCurrencyChange(it.code); open = false },
            onDismiss = { open = false }
        )
    }
}

package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.util.ThemeControls

@Composable
fun AppTextField(
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier.fillMaxWidth(),
    value: String = "",
    onValueChange: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    ),
    label: String? = null,
    placeholder: String? = null,
    leadingIconPainter: Painter? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        keyboardType = KeyboardType.Text
    ),
) {

    TextField(
        value = value,
        onValueChange = onValueChange ?: {},
        modifier = modifier,
        colors = colors,
        readOnly = readOnly,
        enabled = enabled,
        label = {
            if (label != null)
                Text(label)
        },
        placeholder = {
            if (placeholder != null)
                Text(placeholder)
        },
        leadingIcon = {
            if (leadingIconPainter != null)
                Icon(
                    painter = leadingIconPainter,
                    contentDescription = null,
                )
        },
        keyboardOptions = keyboardOptions,

    )
}

@Composable
fun AppTextField(
    @SuppressLint("ModifierParameter")
    modifier: Modifier = Modifier.fillMaxWidth(),
    value: String = "",
    onValueChange: ((String) -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent
    ),
    @StringRes label: Int? = null,
    @StringRes placeholder: Int? = null,
    @DrawableRes leadingIconPainter: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Words,
        keyboardType = KeyboardType.Text
    )
) {
    val label = if (label != null) stringResource(label) else null
    val placeholder = if (placeholder != null) stringResource(placeholder) else null
    val leadingIconPainter = if (leadingIconPainter != null) painterResource(leadingIconPainter) else null


    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        colors = colors,
        readOnly = readOnly,
        enabled = enabled,
        label = label,
        placeholder = placeholder,
        keyboardOptions = keyboardOptions,
        leadingIconPainter = leadingIconPainter,

    )
}



@Composable
fun TestSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Switch // Helps accessibility maybe?
            )
            .padding(start = 54.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Text(
            text = label,
            //style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            // The Row's toggleable modifier handles the click
            onCheckedChange = null
        )
    }
}


@Composable
fun OptionTile(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    interactionSource: MutableInteractionSource? = null,
    title: String? = null,
    description: String? = null,
    leftIconPainter: Painter? = null,
    leftIconContentDescription: String? = null,
    leftIconRotationAmount: Float? = null,
    rightIconPainter: Painter? = null,
    rightIconContentDescription: String? = null,
    rightIconRotationAmount: Float? = null,
    noIconPadding: Boolean = false,
    onClick: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    checked: Boolean? = null,
    tonalElevation: Dp = 0.dp,
    color: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isSwitch = checked != null && onCheckedChange != null
    val isButton = onClick != null

    val illegalState = isButton && isSwitch

    val color = if (illegalState) MaterialTheme.colorScheme.errorContainer else color
    val contentColor = if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else contentColor


    val rightIconRotationModifier = if (rightIconRotationAmount != null) {
        val rotationState by animateFloatAsState(
            targetValue = rightIconRotationAmount,
            label = "rightIconRotation"
        )
        Modifier.rotate(rotationState)
    } else {
        Modifier
    }

    val behaviorModifier = if(illegalState) {
        Modifier
    } else if (isSwitch) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = interactionSource,
            //indication = LocalIndication.current // TODO: What happens with the default value?
        )
    } else if (isButton) {
        Modifier.clickable(
            onClick = onClick,
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            //indication = LocalIndication.current
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .then(behaviorModifier)
            .heightIn(min = 56.dp),
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation
        //interactionSource = interactionSource

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(),
        ) {
            if (leftIconPainter != null) {
                Icon(
                    painter = leftIconPainter,
                    contentDescription = leftIconContentDescription,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(24.dp)
                )
            } else if (!noIconPadding) {
                Box(
                    modifier = Modifier
                        .padding(14.dp)
                        .size(24.dp)
                )
            } else {
                Box(Modifier.padding(start = 14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        title,
                        textAlign = TextAlign.Left,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (description != null || illegalState) {
                    if (illegalState) {
                        Text(
                            "This tile is in illegal state,\n" +
                                    "please report this to the developer."
                        )
                    } else {
                        Text(
                            description!!,
                            textAlign = TextAlign.Left,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }


            if (checked != null) {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    Modifier.padding(14.dp)
                )
            } else if (rightIconPainter != null) {
                Icon(
                    painter = rightIconPainter,
                    contentDescription = rightIconContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .then(rightIconRotationModifier)
                        .padding(14.dp)
                        .size(24.dp)
                )
            }


        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    //topBar: @Composable () -> Unit = {},
    topAppBarTitle: String = "",
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    topNavigationOnClick: (() -> Unit)? = null,
    topNavigationActive: Boolean = true,
    topActionOnClick: (() -> Unit)? = null,
    topActionActive: Boolean = true,

    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

        // Bars
        topBar = {
            Column() {
                TopAppBar(
                    title = {
                        Text(topAppBarTitle)
                    },
                    navigationIcon = {
                        if (topNavigationOnClick != null) {
                            IconButton(
                                onClick = topNavigationOnClick,
                                enabled = topNavigationActive
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, null)
                            }
                        }
                    },
                    actions = {
                        if (topActionOnClick != null) {
                            IconButton(
                                onClick = topActionOnClick,
                                enabled = topActionActive
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Done,
                                    contentDescription = "Localized description"
                                )
                            }
                        }

                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
                HorizontalDivider()
            }
        },

        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,


        // Passing through
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}

@Composable
fun HorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = ThemeControls.DividerThickness.dp,
    color: Color = DividerDefaults.color,
) {
    if (ThemeControls.showDividers) {
        MaterialHorizontalDivider(modifier, thickness, color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDropDownMenu(
    label: String? = null,
    options: List<String>,
    initialIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    focusManager: FocusManager = LocalFocusManager.current
) {
    var expanded by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState(options[initialIndex])

    ExposedDropdownMenuBox(expanded, {expanded = it}) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            state = textFieldState,
            readOnly = true,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { if (label != null) Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MenuDefaults.containerColor,
            shape = MenuDefaults.shape,
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd(option)
                        onSelectedIndexChange(index)
                        focusManager.clearFocus()
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

// DIALOGS

@Composable
fun SingleSelectDialog(
    title: String? = null,
    text: String? = null,
    items: List<String>? = null,
    initialSelectedIndex: Int? = null,
    onSelect: (Int) -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
        ) {
            Column(
                modifier = Modifier
                    //.padding(24.dp)
                    .widthIn(min = 280.dp, max = 560.dp)
            ) {
                Column(
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    if (title != null && text != null) {
                        Spacer(Modifier.padding(2.dp))
                    }
                    if (text != null) {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp)

                // Scrollable column instead of LazyColumn to avoid overload issues
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    items?.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .clickable(
                                    role = Role.RadioButton,
                                    onClick = {
                                        selectedIndex = index
                                        onSelect(index)
                                        onDismiss()
                                    }
                                )
                                .padding(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = index == selectedIndex,
                                onClick = null,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )

                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 24.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}


@Composable
fun CustomDialog(
    title: String? = null,
    description: String? = null,
    onDismissRequest: () -> Unit,
    onOk: () -> Unit,

    okText: String = stringResource(android.R.string.ok),
    cancelText: String = stringResource(android.R.string.cancel),
    content: @Composable (() -> Unit)? = null
) {

    val properties = DialogProperties()


    Dialog(
        onDismissRequest,
        properties,
    ) {
        val focusManager = LocalFocusManager.current
        Surface(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        focusManager.clearFocus()
                    })
            },
            MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (content != null) Spacer(Modifier.padding(4.dp))
                HorizontalDivider()
                if (content != null) {
                    Surface(
                        Modifier
                            .weight(1f, false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        content()
                    }
                    HorizontalDivider()
                    Spacer(Modifier.padding(4.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onDismissRequest) {
                        Text(cancelText)
                    }
                    TextButton(onOk) {
                        Text(okText)
                    }
                }
            }
        }
    }
}

// PREVIEWS

@Preview
@Composable
fun AppScaffoldPreview() {
    AppScaffold(topAppBarTitle = "Preview") { innerPadding ->
        Box(
            Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Content")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptionTilePreview() {
    var checked = true
    OptionTile(
        leftIconPainter = painterResource(R.drawable.ic_label_outline_24dp),
        title = "Content",
        checked = checked,
        onCheckedChange = {checked = !checked}
    )
}

@Preview
@Composable
fun SingleSelectDialogPreview() {
    SingleSelectDialog(
        "Title",
        "Description 1 2 3!",
        listOf("1", "2", "3", "Lorem ipsum dolor sit amet")
    )
}

@Preview
@Composable
fun CustomDialogPreview() {
    CustomDialog("Title", "Description", {}, {}) {
        Text("Content")
    }
}
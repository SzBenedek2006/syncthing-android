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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineSeries
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import dev.benedek.syncthingandroid.R
import dev.benedek.syncthingandroid.ui.theme.SyncthingandroidTheme
import dev.benedek.syncthingandroid.util.ThemeControls
import kotlinx.coroutines.runBlocking
import androidx.compose.material3.HorizontalDivider as MaterialHorizontalDivider

val dialogTonalElevation = 6.dp

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
		enabled = enabled,
		readOnly = readOnly,
		label = { if (label != null) Text(label) },
		placeholder = { if (placeholder != null) Text(placeholder) },
		leadingIcon = {
			if (leadingIconPainter != null) Icon(leadingIconPainter, null)
		},
		keyboardOptions = keyboardOptions,
		colors = colors
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
	val leadingIconPainter =
		if (leadingIconPainter != null) painterResource(leadingIconPainter) else null


	AppTextField(
		modifier,
		value,
		onValueChange,
		readOnly,
		enabled,
		colors,
		label,
		placeholder,
		leadingIconPainter,
		keyboardOptions
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
				role = Role.Switch,
				onValueChange = onCheckedChange
			)
			.padding(54.dp, 12.dp, 16.dp, 12.dp)
	) {
		Text(
			text = label,
			modifier = Modifier.weight(1f),
			color = MaterialTheme.colorScheme.onBackground
		)
		// Row's toggleable modifier handles the switch
		Switch(checked = checked, onCheckedChange = null)
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
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	titleColor: Color = contentColor,
	descriptionColor: Color = contentColor,
	iconColor: Color = contentColor,
	titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
	descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
	titleWeight: FontWeight? = titleStyle.fontWeight,
	descriptionWeight: FontWeight? = descriptionStyle.fontWeight
) {
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val isSwitch = checked != null && onCheckedChange != null
	val isButton = onClick != null

	val illegalState = isButton && isSwitch

	val color = if (illegalState) MaterialTheme.colorScheme.errorContainer else color
	val contentColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else contentColor
	val titleColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else titleColor
	val descriptionColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else descriptionColor


	val rightIconRotationModifier = if (rightIconRotationAmount != null) {
		val rotationState by animateFloatAsState(
			targetValue = rightIconRotationAmount,
			label = "rightIconRotation"
		)
		Modifier.rotate(rotationState)
	} else {
		Modifier
	}

	val behaviorModifier = if (illegalState) {
		Modifier
	} else if (isSwitch) {
		Modifier.toggleable(
			value = checked,
			enabled = enabled,
			role = Role.Switch,
			interactionSource = interactionSource,
			onValueChange = onCheckedChange,
			indication = null
		)
	} else if (isButton) {
		Modifier.clickable(
			enabled = enabled,
			role = Role.Button,
			interactionSource = interactionSource,
			onClick = onClick
		)
	} else {
		Modifier
	}

	Surface(
		modifier.then(behaviorModifier).heightIn(min = 56.dp),
		shape,
		color,
		contentColor,
		tonalElevation
	) {
		Row(Modifier.padding(), verticalAlignment = Alignment.CenterVertically) {
			if (leftIconPainter != null) {
				Icon(
					leftIconPainter,
					leftIconContentDescription,
					Modifier.padding(14.dp).size(24.dp),
					iconColor
				)
			} else if (!noIconPadding) {
				Box(Modifier.padding(14.dp).size(24.dp))
			} else {
				Box(Modifier.padding(start = 14.dp))
			}
			Column(Modifier.weight(1f)) {
				if (title != null) {
					Text(
						text = title,
						textAlign = TextAlign.Left,
						style = titleStyle,
						fontWeight = titleWeight,
						color = titleColor
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
							text = description!!,
							textAlign = TextAlign.Left,
							style = descriptionStyle,
							fontWeight = descriptionWeight,
							color = descriptionColor
						)
					}
				}
			}

			if (checked != null) {
				Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(14.dp))
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


@Composable
fun StatTile(
	modifier: Modifier = Modifier,
	title: String? = null,
	description: String? = null,
	chart: @Composable (() -> Unit)? = null,
	enabled: Boolean = true,
	shape: Shape = RectangleShape,
	interactionSource: MutableInteractionSource? = null,
	leftIcon: @Composable (() -> Unit)? = null,
	rightIcon: @Composable (() -> Unit)? = null,
	noIconPadding: Boolean = false,
	onClick: (() -> Unit)? = null,
	onCheckedChange: ((Boolean) -> Unit)? = null,
	checked: Boolean? = null,
	tonalElevation: Dp = 0.dp,
	color: Color = Color.Transparent,
	contentColor: Color = MaterialTheme.colorScheme.onSurface,
	titleColor: Color = contentColor,
	descriptionColor: Color = contentColor,
	titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
	descriptionStyle: TextStyle = MaterialTheme.typography.bodyMedium,
	titleWeight: FontWeight? = titleStyle.fontWeight,
	descriptionWeight: FontWeight? = descriptionStyle.fontWeight
) {
	val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
	val isSwitch = checked != null && onCheckedChange != null
	val isButton = onClick != null

	val illegalState = isButton && isSwitch

	val color = if (illegalState) MaterialTheme.colorScheme.errorContainer else color
	val contentColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else contentColor
	val titleColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else titleColor
	val descriptionColor =
		if (!enabled) ButtonDefaults.buttonColors().disabledContentColor else descriptionColor


	val behaviorModifier = if (illegalState) {
		Modifier
	} else if (isSwitch) {
		Modifier.toggleable(
			value = checked,
			enabled = enabled,
			role = Role.Switch,
			interactionSource = interactionSource,
			onValueChange = onCheckedChange
		)
	} else if (isButton) {
		Modifier.clickable(
			enabled = enabled,
			role = Role.Button,
			interactionSource = interactionSource,
			onClick = onClick
		)
	} else {
		Modifier
	}

	Surface(
		modifier.then(behaviorModifier).heightIn(min = 56.dp),
		shape,
		color,
		contentColor,
		tonalElevation
	) {
		Box {
			if (chart != null) {
				Box(
					Modifier
						.matchParentSize()
						.graphicsLayer {
							compositingStrategy = CompositingStrategy.Offscreen
						}
						.drawWithContent {
							drawContent()
							drawRect(
								brush = Brush.horizontalGradient(
									0.0f to Color.Transparent,
									0.3f to Color.Black.copy(0.2f),
									1.0f to Color.Black // fully opaque
								),
								blendMode = BlendMode.DstIn
							)
						}
				) {
					chart()
				}
			}
			Row(Modifier.padding(), verticalAlignment = Alignment.CenterVertically) {
				if (leftIcon != null) {
					leftIcon()
				} else if (!noIconPadding) {
					Box(Modifier.padding(14.dp).size(24.dp))
				} else {
					Box(Modifier.padding(start = 14.dp))
				}
				Column(Modifier.weight(1f).padding(4.dp)) {
					if (title != null) {
						Text(
							text = title,
							textAlign = TextAlign.Left,
							style = titleStyle,
							fontWeight = titleWeight,
							color = titleColor
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
								text = description!!,
								textAlign = TextAlign.Left,
								style = descriptionStyle,
								fontWeight = descriptionWeight,
								color = descriptionColor
							)
						}
					}
				}

				if (checked != null) {
					Switch(checked = checked, onCheckedChange = null, modifier = Modifier.padding(14.dp))
				} else {
					rightIcon?.invoke()
				}
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
	topNavigationIcon: ImageVector? = null,
	topActionOnClick: (() -> Unit)? = null,
	topActionActive: Boolean = true,

	content: @Composable (PaddingValues) -> Unit,
) {
	val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

	Scaffold(
		modifier.nestedScroll(scrollBehavior.nestedScrollConnection),

		// Bars
		{
			Column {
				TopAppBar(
					title = { Text(topAppBarTitle) },
					navigationIcon = {
						if (topNavigationOnClick != null) {
							IconButton(
								onClick = topNavigationOnClick,
								enabled = topNavigationActive
							) {
								Icon(
									topNavigationIcon ?: Icons.AutoMirrored.Outlined.ArrowBack,
									stringResource(R.string.menu)
								)
							}
						}
					},
					actions = {
						if (topActionOnClick != null) {
							IconButton(
								onClick = topActionOnClick,
								enabled = topActionActive
							) {
								Icon(Icons.Filled.Done, "Localized description")
							}
						}
					},
					colors = TopAppBarDefaults.topAppBarColors(
						containerColor = MaterialTheme.colorScheme.background,
						titleContentColor = MaterialTheme.colorScheme.onBackground,
						scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
					),
					scrollBehavior = scrollBehavior
				)
				HorizontalDivider()
			}
		},
		bottomBar,
		snackbarHost,
		floatingActionButton,


		// Passing through
		floatingActionButtonPosition,
		containerColor,
		contentColor,
		contentWindowInsets,
		content
	)
}

@Composable
fun HorizontalDivider(
	modifier: Modifier = Modifier,
	thickness: Dp = ThemeControls.dividerThickness.dp,
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

	ExposedDropdownMenuBox(expanded, { expanded = it }) {
		OutlinedTextField(
			state = textFieldState,
			modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
			readOnly = true,
			label = { if (label != null) Text(label) },
			trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
			lineLimits = TextFieldLineLimits.SingleLine
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
					contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
			Modifier,
			MaterialTheme.shapes.extraLarge,
			tonalElevation = dialogTonalElevation
		) {
			Column(Modifier.widthIn(280.dp, 560.dp)) {
				Column(Modifier.padding(24.dp, 24.dp, 24.dp, 12.dp)) {
					if (title != null) {
						Text(title, style = MaterialTheme.typography.titleLarge)
					}
					if (title != null && text != null) {
						Spacer(Modifier.padding(2.dp))
					}
					if (text != null) {
						Text(text, style = MaterialTheme.typography.bodyMedium)
					}
				}

				HorizontalDivider(thickness = 1.dp)

				// Scrollable column instead of LazyColumn to avoid overload issues
				Column(
					Modifier
						.fillMaxWidth()
						.heightIn(max = 320.dp)
						.verticalScroll(rememberScrollState())
				) {
					items?.forEachIndexed { index, label ->
						Row(
							Modifier
								.clickable(
									role = Role.RadioButton,
									onClick = {
										selectedIndex = index
										onSelect(index)
										onDismiss()
									}
								)
								.padding(),
							verticalAlignment = Alignment.CenterVertically
						) {
							RadioButton(
								index == selectedIndex,
								null,
								Modifier.padding(24.dp, 8.dp)
							)
							Text(
								text = label,
								modifier = Modifier.weight(1f),
								style = MaterialTheme.typography.bodyLarge
							)
						}
					}
				}

				HorizontalDivider(thickness = 1.dp)

				Row(Modifier.fillMaxWidth(), Arrangement.End) {
					TextButton(
						onDismiss,
						Modifier.padding(end = 24.dp, top = 12.dp, bottom = 12.dp)
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
	onOk: (() -> Unit)?,

	okText: String = stringResource(android.R.string.ok),
	cancelText: String = stringResource(android.R.string.cancel),
	content: @Composable (() -> Unit)? = null
) {

	val properties = DialogProperties()


	Dialog(onDismissRequest, properties) {
		val focusManager = LocalFocusManager.current
		Surface(
			Modifier
				.fillMaxWidth()
				.pointerInput(Unit) {
					detectTapGestures(onPress = { focusManager.clearFocus() })
				},
			MaterialTheme.shapes.extraLarge,
			tonalElevation = dialogTonalElevation
		) {
			Column(Modifier.padding(24.dp)) {
				if (title != null) {
					Text(title, style = MaterialTheme.typography.titleLarge)
				}
				if (description != null) {
					Text(description, style = MaterialTheme.typography.bodyMedium)
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
				Row(Modifier.fillMaxWidth(), Arrangement.End) {
					TextButton(onDismissRequest) { Text(cancelText) }
					if (onOk != null) {
						TextButton(onOk) { Text(okText) }
					}
				}
			}
		}
	}
}


// CHARTS


@Composable
fun ComposeBasicLineChart(
	values: List<Number>,
	modifier: Modifier = Modifier,
	maxValue: Number = values.maxOf { it.toDouble() }
) {
	val modelProducer = remember { CartesianChartModelProducer() }
	LaunchedEffect(values, maxValue) {
		modelProducer.runTransaction {
			// Learn more: https://patrykandpatrick.com/z5ah6v.
			lineSeries { series(values) }
		}
	}
	ComposeBasicLineChart(modelProducer, maxValue, modifier)
}

@Composable
private fun ComposeBasicLineChart(
	modelProducer: CartesianChartModelProducer,
	maxValue: Number,
	modifier: Modifier = Modifier,
) {
	val color = MaterialTheme.colorScheme.primary

	CartesianChartHost(
		chart =
			rememberCartesianChart(
				rememberLineCartesianLayer(
					rangeProvider = CartesianLayerRangeProvider.fixed(
						minY = 0.0,
						maxY = if (maxValue.toDouble() == 0.0) 1.0 else maxValue.toDouble()
					),
					lineProvider = LineCartesianLayer.LineProvider.series(
						LineCartesianLayer.rememberLine(
							fill = LineCartesianLayer.LineFill.single(
								Fill(color)
							),
							stroke = LineCartesianLayer.LineStroke.Continuous(3.dp),
							areaFill = LineCartesianLayer.AreaFill.single(
								Fill(
									Brush.verticalGradient(
										listOf(
											color.copy(alpha = 0.4f),
											Color.Transparent
										)
									)
								)
							),
							interpolator = LineCartesianLayer.Interpolator.cubic(0.7f)
						)
					)
				),
				startAxis = null, //VerticalAxis.rememberStart(guideline = null, tick = null, line = null, itemPlacer = VerticalAxis.ItemPlacer.count({ 3 })),
				bottomAxis = null,
			),
		modelProducer = modelProducer,
		scrollState = rememberVicoScrollState(scrollEnabled = false),
		animationSpec = null,
		modifier = modifier,
	)
}


// PREVIEWS
@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun AppScaffoldPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		AppScaffold(Modifier, "Preview") { innerPadding ->
			Box(
				Modifier
					.padding(innerPadding)
					.fillMaxSize(),
				Alignment.Center
			) {
				Text("Content")
			}
		}
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun AppTextFieldPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			AppTextField(
				Modifier.padding(16.dp),
				"Test Value",
				label = "Label",
				placeholder = "Placeholder"
			)
		}
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun TestSwitchPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { TestSwitch("Test Switch", true, {}) }
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun OptionTilePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			var checked by remember { mutableStateOf(true) }
			OptionTile(
				title = "Content",
				leftIconPainter = painterResource(R.drawable.ic_label_outline_24dp),
				checked = checked,
				onCheckedChange = { checked = !checked }
			)
		}
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun StatTilePreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface {
			var checked by remember { mutableStateOf(true) }
			StatTile(
				title = "Content",
				leftIcon = {
					Icon(
						painterResource(R.drawable.ic_label_outline_24dp),
						null,
						Modifier.padding(14.dp)
					)
				},
				checked = checked,
				onCheckedChange = { checked = !checked }
			)
		}
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun HorizontalDividerPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface(Modifier.padding(16.dp)) { HorizontalDivider() }
	}
}

@Preview(showBackground = true, uiMode = ThemeControls.UI_MODE)
@Composable
fun AppDropDownMenuPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface(Modifier.padding(16.dp)) {
			AppDropDownMenu(
				"Select Option",
				listOf("Option 1", "Option 2", "Option 3"),
				0,
				{}
			)
		}
	}
}

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun SingleSelectDialogPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		SingleSelectDialog(
			"Title",
			"Description 1 2 3!",
			listOf("1", "2", "3", "Lorem ipsum dolor sit amet")
		)
	}
}

@Preview(uiMode = ThemeControls.UI_MODE)
@Composable
fun CustomDialogPreview() {
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		CustomDialog("Title", "Description", {}, {}) {
			Text("Content")
		}
	}
}

@Composable
@Preview(uiMode = ThemeControls.UI_MODE)
private fun ComposeBasicLineChartPreview() {
	val modelProducer = remember { CartesianChartModelProducer() }
	// Use `runBlocking` only for previews, which don’t support asynchronous execution.
	runBlocking {
		modelProducer.runTransaction {
			// Learn more: https://patrykandpatrick.com/z5ah6v.
			lineSeries { series(13, 8, 7, 12, 1, 15, 14, 0, 11, 6, 12, 0, 11, 12, 11) }
		}
	}
	SyncthingandroidTheme(ThemeControls.PREVIEW_DARK_THEME, ThemeControls.isMonetEnabled) {
		Surface { ComposeBasicLineChart(modelProducer, 25) }
	}
}

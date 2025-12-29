package dev.benedek.syncthingandroid.ui.reusable

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

import dev.benedek.syncthingandroid.R


@SuppressLint("ModifierParameter")
@Composable
fun SlideWelcomeTitle(
    text: String,
    modifier: Modifier = Modifier.padding(30.dp),
    fontSize: TextUnit = 30.sp,
    textAlign: TextAlign? = TextAlign.Center,
    lineHeight: TextUnit = 30.sp
) {
    Text(
        text = text,
        fontSize = fontSize,
        textAlign = textAlign,
        lineHeight = lineHeight,
        modifier = modifier

    )
}

@SuppressLint("ModifierParameter")
@Composable
fun SlideTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = dimensionResource(R.dimen.slide_title).value.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign? = TextAlign.Center
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textAlign = textAlign
    )
}

@SuppressLint("ModifierParameter")
@Composable
fun SlideDescription(
    text: String,
     modifier: Modifier = Modifier
        .padding(
            top = dimensionResource(R.dimen.desc_marginTop),
            bottom = dimensionResource(R.dimen.desc_paddingBottom),
            start = dimensionResource(R.dimen.desc_padding),
            end = dimensionResource(R.dimen.desc_padding)
        ),
    textAlign: TextAlign = TextAlign.Center,
    fontSize: TextUnit = dimensionResource(R.dimen.slide_desc).value.sp,
    lineHeight: TextUnit = 16.sp,

    ) {
    Text(
        text = text,
        textAlign = textAlign,
        fontSize = fontSize,
        lineHeight = lineHeight,
        modifier = modifier
    )
}

@SuppressLint("ModifierParameter")
@Composable
fun ImageSlide(
    painter: Painter,
    modifier: Modifier = Modifier.size(size = dimensionResource(R.dimen.img_width_height)),
    contentDescription: String? = null,
    colorFilter: ColorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier,
        colorFilter = colorFilter
    )
}

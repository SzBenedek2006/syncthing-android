package dev.benedek.syncthingandroid.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benedek.syncthingandroid.R

// Reimplementation of the activity_firststart_slide_intro.xml

@Composable
fun IntroSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = R.dimen.dots_full_height.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_text),
            fontSize = 30.sp,
            modifier = Modifier.padding(30.dp),
            textAlign = TextAlign.Center
        )
        Image(
            painter = painterResource(id = R.drawable.ic_monochrome),
            contentDescription = null,
            modifier = Modifier.size(size = R.dimen.img_width_height.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.introduction),
            fontSize = R.dimen.slide_title.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.welcome_text),
            textAlign = TextAlign.Center,
            fontSize = R.dimen.slide_desc.sp,
            modifier = Modifier
                .padding(
                    top = R.dimen.desc_marginTop.dp,
                    bottom = R.dimen.desc_paddingBottom.dp,
                    start = R.dimen.desc_padding.dp,
                    end = R.dimen.desc_padding.dp
                )
        )
    }
}

@Preview
@Composable
fun IntroSlidePreview() {
    IntroSlide()
}

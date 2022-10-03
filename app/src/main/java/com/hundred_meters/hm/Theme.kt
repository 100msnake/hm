package com.hundred_meters.hm

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// https://developer.android.com/codelabs/jetpack-compose-theming#0


@Preview
@Composable
fun Shit(){
    MFTheme {
        Surface()
        {
            Screen()
        }
    }
    }

@Preview
@Composable
fun DarkShit(){
    MFTheme( darkTheme = true ) {
        Surface()
        {
            Screen()
        }
    }
}


@Composable
fun MFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        typography = MFTypography,
        shapes = MFShapes,
        content = content
    )
}



val MFShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp)
)

// FONTS
// put new fonts in the res > font folder, then link them here,
// then use them in defined styles below

private val Montserrat = FontFamily(
    Font(R.font.montserrat_regular),
    Font(R.font.montserrat_medium, FontWeight.W500),
    Font(R.font.montserrat_semibold, FontWeight.W600)
)

val MFTypography = Typography(
    h4 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W600,
        fontSize = 30.sp
    ),
    h5 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp
    ),
    h6 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp
    ),
    subtitle1 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp
    ),
    subtitle2 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    // body1 used as topic text, so regular
    body1 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    // body2 used as topic text, so bold ad smaller
    body2 = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    overline = TextStyle(
        fontFamily = Montserrat,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp
    )
)


val Red700 = Color(0xffdd0d3c)
val Red800 = Color(0xffd00036)
val Red900 = Color(0xffc20029)
val Red200 = Color(0xfff297a2)
val Red300 = Color(0xffea6d7e)

// you need to add 0xff to the start of hex numbers, just for unnecessary ass pain

val light_gray = Color(0xffc1d5e0)
val medium_gray = Color(0xff90a4ae)
val dark_gray = Color(0xff62757f)

val sky_blue = Color(0xff00B5E2)
val sea_blue = Color(0xff006994)

private val LightColors = lightColors(
    primary = sky_blue,
    primaryVariant = sky_blue, //light_gray,
    onPrimary = Color.White,
    secondary = sky_blue, //medium_gray,
    secondaryVariant = sky_blue, //dark_gray,
    onSecondary = Color.White,
    error = Red800
)

private val DarkColors = darkColors(
    primary = sky_blue,
    primaryVariant = sky_blue, //dark_gray,
    onPrimary = Color.Black,
    secondary = sky_blue, // medium_gray,
    secondaryVariant = sky_blue, //light_gray,
    onSecondary = Color.Black,
    error = Red200,
)


// TODO button color


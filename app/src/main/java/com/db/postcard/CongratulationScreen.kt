package com.db.postcard

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.toSize
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit


@Composable
fun CongratulationScreen() {
    Box {
        ImageTextButton()
        Konfetti()
    }
}

@Composable
fun ImageTextButton(){

    (LocalView.current.context as Activity).window.statusBarColor = CharacterManager.gradient[0].toArgb()
    val context = LocalContext.current

    val audio = CharacterManager.sound
    MyMediaPlayer.start(audio!!)
    Column(
        Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(colors = CharacterManager.gradient))
    ) {
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxSize(),
            Alignment.Center
        ) {
            Column() {
                if(CharacterManager.stage == Stage.COMPLETE){ ExpandedCharacters(context = context) }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                    AnimateVisibility {
                        Image(
                            bitmap = CharacterManager.image.asImageBitmap(),
                            contentDescription = CharacterManager.currentCharacter,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(20.dp))
                                .shadow(
                                    elevation = 8.dp, shape = RoundedCornerShape(20.dp), clip = true
                                )
                                .border(
                                    width = 4.dp,
                                    brush = Brush.verticalGradient(CharacterManager.gradient.reversed()),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        )
                    }

                }
            }
        }

        Box(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxSize()
                .padding(horizontal = 50.dp),
            Alignment.Center
        ) {
            AnimateVisibility {
                Text(
                    text = CharacterManager.text,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    color = CharacterManager.fontColor
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(0.1f)
                .fillMaxSize(),
            Alignment.Center
        ) {
            if(CharacterManager.isButton){
                Button(
                    colors = ButtonDefaults.buttonColors(CharacterManager.gradient[0]),
                    modifier = Modifier
                        .background(
                            brush = Brush.verticalGradient(CharacterManager.gradient.reversed()),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .shadow(
                            elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = true
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = 4.dp,
                            brush = Brush.verticalGradient(CharacterManager.gradient),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .size(200.dp, 50.dp),
                    onClick = {
                        MyMediaPlayer.stop()
                        CharacterManager.nextCharacter(context)
                    }) {
                    Text("\uD83C\uDF89")
                }
            }

        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedCharacters(context: Context){
    var expanded by remember { mutableStateOf(false) }
    val characterList = context.assets.list("characters")!!.toList()
    var selectedCharacter = CharacterManager.currentCharacter

    var textFieldSize by remember { mutableStateOf(Size.Zero)}

    val icon = if (expanded)
        Icons.Filled.ArrowDropUp
    else
        Icons.Filled.ArrowDropDown

    Column(modifier = Modifier.clickable { expanded = !expanded }) {
        OutlinedTextField(
            enabled = false,
            value = selectedCharacter,
            onValueChange = { selectedCharacter = it },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    textFieldSize = coordinates.size.toSize()
                },
            label = {Text("Выбирай", style = TextStyle(CharacterManager.fontColor))},
            trailingIcon = {
                Icon(icon,"contentDescription",
                    Modifier.clickable { expanded = !expanded })
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current){textFieldSize.width.toDp()})
        ) {
            characterList.forEach { character ->
                DropdownMenuItem(
                    text = { Text(character) },
                    onClick = {
                        selectedCharacter = character
                        expanded = false
                        CharacterManager.selectCharacter(context, character)
                        MyMediaPlayer.stop()
                        MyMediaPlayer.start(CharacterManager.sound!!)
                    })
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimateVisibility(content: @Composable() AnimatedVisibilityScope.() -> Unit) {
    AnimatedVisibility(
        visible = !CharacterManager.isTransit,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut(),
        content = content
    )
}

@Composable
fun Konfetti(){
    KonfettiView(parties = listOf(
        Party(
            speed = 0f,
            maxSpeed = 5f,
            damping = 0.9f,
            angle = Angle.BOTTOM,
            spread = Spread.ROUND,
            colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def, Color.Green.toArgb()) +
                    CharacterManager.gradient.map { it.toArgb() },
            emitter = Emitter(duration = 0, TimeUnit.SECONDS).perSecond(50),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
        )), modifier = Modifier.fillMaxSize())
}



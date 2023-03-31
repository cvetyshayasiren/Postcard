package com.db.postcard

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.fixedRateTimer
import kotlin.math.abs


object CharacterManager{
    private var isInit: Boolean = false
    private var characterList: List<String> = listOf()
    private var nextFontColor: Color = Color.White

    var currentCharacter: String by mutableStateOf("")
    var stage: Stage by mutableStateOf(Stage.START)
    var fontColor: Color by mutableStateOf(Color.White)
    var text: String by mutableStateOf("")
    var image: Bitmap by mutableStateOf(Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888))
    var gradient: List<Color> by mutableStateOf(listOf(Color.Transparent, Color.Transparent))
    var sound: AssetFileDescriptor? by mutableStateOf(null)
    var isTransit: Boolean by mutableStateOf(false)
    var isButton: Boolean by mutableStateOf(true)


    fun init(context: Context){
        if(!isInit){
            fromDataStore(context)
            if(stage == Stage.START){ currentCharacter = "start" }
            updateCharacterData(context)
            isInit = true
        }
    }

    fun selectCharacter(context: Context, character: String) {
        currentCharacter = character
        updateCharacterData(context)
    }

    fun nextCharacter(context: Context) {
        checkList(context)
        currentCharacter = characterList[0]
        characterList = characterList.drop(1)
        checkStage()
        onDataStore(context)
        updateCharacterData(context)
    }

    private fun checkList(context: Context){
        if(characterList.isEmpty()){
            val preparingList: List<String> = context.assets.list("characters")!!.toList()
                .shuffled().filter { it !in listOf("start", "finish") }
            characterList = when(stage){
                Stage.START -> preparingList + listOf("finish")
                else -> preparingList
            }

        }
    }

    private fun checkStage(){
        when(stage){
            Stage.START -> stage = Stage.INCOMPLETE
            Stage.INCOMPLETE -> if(currentCharacter == "finish"){stage = Stage.FINISH}
            Stage.FINISH -> stage = Stage.COMPLETE
            else -> return
        }
    }

    private fun updateCharacterData(context: Context){
        isTransit = true
        isButton = false
        val startGradient = gradient
        val nextText = receiveText(context)
        val nextImage = receiveImage(context)
        sound = receiveSound(context)
        val targetGradient = receiveGradient(nextImage)
        CoroutineScope(Dispatchers.Main).launch { colorTransition(startGradient, targetGradient, nextText, nextImage) }
    }

    private fun onDataStore(context: Context) {
        runBlocking {
            context.CharacterListDataStore.updateData { currentSettings ->
                currentSettings
                    .toBuilder()
                    .clear()
                    .addAllCharacter(characterList)
                    .setCurrent(currentCharacter)
                    .setStage(stage)
                    .build()
            }
        }
    }

    private fun fromDataStore(context: Context){
        characterList = runBlocking { context.CharacterListDataStore.data.map { settings -> settings.characterList }.first() }
        currentCharacter = runBlocking { context.CharacterListDataStore.data.first().current }
        stage = runBlocking { context.CharacterListDataStore.data.first().stage }
    }


    private fun receiveText(context: Context): String{
        val inputStream: InputStream =
            context.assets.open("characters/$currentCharacter/text.txt")
        val size: Int = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        return String(buffer)
    }

    private fun receiveImage(context: Context): Bitmap{
        val bitmap: Bitmap? = context.assetsToBitmap("characters/$currentCharacter/pic.png")
        return bitmap!!
    }

    private fun receiveGradient(bitmap: Bitmap): List<Color>{
        val width = bitmap.width
        val height = bitmap.height
        var red = .0f
        var green = .0f
        var blue = .0f
        for (n in 0..1000) {
            val x = (0 until width).random()
            val y = (0 until height).random()
            val pixel = bitmap.getPixel(x, y)
            red += pixel.red
            green += pixel.green
            blue += pixel.blue
        }

        val rgb = Color(
            (red / 1000).toInt(),
            (green / 1000).toInt(),
            (blue / 1000).toInt()
        )

        val sRed = (rgb.red + .1f).coerceAtMost(1f)
        val sGreen = (rgb.green + .1f).coerceAtMost(1f)
        val sBlue = (rgb.blue + .1f).coerceAtMost(1f)

        val eRed = (rgb.red - .1f).coerceAtLeast(0f)
        val eGreen = (rgb.green - .1f).coerceAtLeast(0f)
        val eBlue = (rgb.blue - .1f).coerceAtLeast(0f)
        calculateFontColor(rgb)

        return listOf(Color(sRed, sGreen, sBlue), Color(eRed, eGreen, eBlue))
    }

    private fun calculateFontColor(rgb: Color){
        val brightness = 0.299*rgb.red + 0.587*rgb.green + 0.114*rgb.blue
        nextFontColor = if(brightness < 0.8) Color.White else Color.Black
    }

    private fun receiveSound(context: Context): AssetFileDescriptor {
        return try {
            context.assets.openFd("characters/$currentCharacter/snd.mp3")
        } catch (e: FileNotFoundException){
            context.assets.openFd("HappyB.mp3")
        }
    }

    private suspend fun colorTransition(startColor: List<Color>,
                                        targetColor: List<Color>,
                                        nextText: String,
                                        nextImage: Bitmap,
                                        frames: Int = 30,
                                        duration: Int = 1000){

        val rTransitListUp = transitionListOfFloat(startColor[0].red, targetColor[0].red, frames)
        val gTransitListUp = transitionListOfFloat(startColor[0].green, targetColor[0].green, frames)
        val bTransitListUp = transitionListOfFloat(startColor[0].blue, targetColor[0].blue, frames)
        val rTransitListDown = transitionListOfFloat(startColor[1].red, targetColor[1].red, frames)
        val gTransitListDown = transitionListOfFloat(startColor[1].green, targetColor[1].green, frames)
        val bTransitListDown = transitionListOfFloat(startColor[1].blue, targetColor[1].blue, frames)

        val delay: Long = (duration / frames).toLong()
        repeat(frames){
            gradient = listOf(
                Color(rTransitListUp[it], gTransitListUp[it], bTransitListUp[it]),
                Color(rTransitListDown[it], gTransitListDown[it], bTransitListDown[it]))
            delay(delay)
            if(it == frames/2){
                image = nextImage
                text = nextText
                fontColor = nextFontColor
                isTransit = false
            }
        }
        isButton = true
    }

    private fun transitionListOfFloat(from: Float, to: Float, parts: Int): List<Float>{
        return if(from < to){
            val part = (to - from) / parts
            List(parts){(from + (it + 1) * part).coerceAtMost(255f)}
        } else{
            val part = (from - to) / parts
            List(parts){(from - (it + 1) * part).coerceAtLeast(0f)}
        }
    }
}


private fun Context.assetsToBitmap(fileName: String): Bitmap? {
    return try {
        with(assets.open(fileName)) {
            BitmapFactory.decodeStream(this)
        }
    } catch (e: IOException) {
        null
    }
}
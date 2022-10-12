package com.practice.tictactoc

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practice.tictactoc.ui.theme.TicTacTocTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TicTacTocTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    var xWins by remember{
                        mutableStateOf<Int>(0)
                    }
                    var oWins by remember{
                        mutableStateOf<Int>(0)
                    }
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ){
                        var winner by remember{
                            mutableStateOf<Player?>(null)
                        }
                        Text(
                            text = "TicTacToc",
                            style = MaterialTheme.typography.h3,
                            color = MaterialTheme.colors.primary,
                            fontSize = 25.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = "$xWins",
                                color = Color.Green,
                                style = MaterialTheme.typography.h3,
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "$oWins",
                                color = Color.Red,
                                style = MaterialTheme.typography.h3,
                            )
                            
                        }

                        TicTacToc(
                            modifier = Modifier.size(400.dp),
                            onPlayerWin = {
                                if(it is Player.X){
                                    xWins++
                                    winner = Player.X
                                } else {
                                    oWins++
                                    winner = Player.O
                                }
                            },
                            onNweRound = {
                                winner = null
                            }

                        )

                        winner?.let{
                           Text(
                               text = "Player ${it.symbol} has Won!",
                               fontSize = 25.sp,
                               color = MaterialTheme.colors.primary,
                           )
                        }

                    }
                }
            }
        }
    }
}

sealed class Player(val symbol:Char){
    object X : Player('X')
    object O : Player('O')

    operator fun not(): Player{
        return if(this is X) O else X
    }
}

@Composable
fun TicTacToc(
    modifier:Modifier = Modifier,
    simbolRate: Float = 0.75f,
    onPlayerWin: (Player)->Unit = {},
    onNweRound: ()-> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var isGameOver by remember{
        mutableStateOf(false)
    }
    var animations = remember{
        emptyAnimations()
    }
    var gameState by remember{
        mutableStateOf(emptyGame())
    }
    var currentPlayer by remember{
        mutableStateOf<Player>(Player.X)
    }
    BoxWithConstraints(modifier = modifier.padding(15.dp)) {

        val thirdOffset = Offset(
            x = this.constraints.maxWidth / 3f,
            y = this.constraints.maxHeight / 3f
        )

        var turns by remember{
            mutableStateOf<Int>(0)
        }

        val tablePath = Path().apply {
            moveTo(thirdOffset.x, 0f)
            lineTo(thirdOffset.x, thirdOffset.y * 3)
            moveTo(thirdOffset.x * 2, 0f)
            lineTo(thirdOffset.x * 2, thirdOffset.y * 3)
            moveTo(0f, thirdOffset.y)
            lineTo(thirdOffset.x * 3, thirdOffset.y)
            moveTo(0f, thirdOffset.y * 2)
            lineTo(thirdOffset.x * 3, thirdOffset.y * 2)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentPlayer) {
                    detectTapGestures {
                        if (turns >= 8) {
                            turns = 0
                            scope.launch {
                                isGameOver = true
                                delay(3000L)
                                isGameOver = false
                                gameState = emptyGame()
                                animations = emptyAnimations()
                                onNweRound()
                            }
                        }
                        turns++
                        if (isGameOver) {
                            return@detectTapGestures
                        }
                        val index: Array<Int> =
                            getTapIndexes(it, Size(size.width.toFloat(), size.height.toFloat()))
                        val i = index[0]
                        val j = index[1]
                        Log.i("BBB", "[i][j] = [$i][$j]")
                        if (gameState[i][j] == 'E') {
                            gameState = updateGameState(gameState, i, j, currentPlayer.symbol)
                            Log.i("CCC", "gameState[$i][$j] = ${gameState[i][j]}")
                            scope.animateFloatToOne(animations[i][j])
                            currentPlayer = !currentPlayer
                        }
                        if (isGameOver(gameState)) {
                            scope.launch {
                                onPlayerWin(!currentPlayer)
                                isGameOver = true
                                turns = 0
                                delay(3000L)
                                isGameOver = false
                                gameState = emptyGame()
                                animations = emptyAnimations()
                                onNweRound()
                            }
                        }

                    }
                }
        ){
            drawPath(path = tablePath, color = Color.Black, style = Stroke(width = 5f))
            val boxWidth2 = ((size.width/3f) * simbolRate)/2f
            val boxHeight2 = ((size.height/3f) * simbolRate)/2f
            gameState.forEachIndexed{ i,row ->
                row.forEachIndexed { j, symbol ->

                    val center = Offset((size.width / 6f)  + j * size.width/3f, (size.height / 6f) + i * size.height/3f)
                    val outPath = Path()
                    val outPath2 = Path()
                    if(symbol == Player.X.symbol){
                        val pathX = Path().apply {
                            moveTo(center.x - boxWidth2, center.y - boxHeight2)
                            lineTo(center.x + boxWidth2, center.y + boxHeight2)
                            moveTo(center.x + boxWidth2, center.y - boxHeight2)
                            lineTo(center.x - boxWidth2, center.y + boxHeight2)
                        }
                        val pathX2 = Path().apply{
                            moveTo(center.x - boxWidth2, center.y + boxHeight2)
                            lineTo(center.x + boxWidth2, center.y - boxHeight2)
                        }

                        PathMeasure().apply {
                            setPath(pathX, false)
                            getSegment(0f, animations[i][j].value * length, outPath)
                        }
                        PathMeasure().apply {
                            setPath(pathX2, false)
                            getSegment(0f, animations[i][j].value * length, outPath2)
                        }

                        drawPath(
                            path =outPath,
                            color = Color.Green,
                            style = Stroke(
                                width = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                        drawPath(
                            path =outPath2,
                            color = Color.Green,
                            style = Stroke(
                                width = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                    else if(symbol == Player.O.symbol){

                        drawArc(
                            color = Color.Red,
                            startAngle = 0f,
                            sweepAngle = animations[i][j].value * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x-boxWidth2,center.y - boxHeight2),
                            size = Size(2 * boxWidth2,2 * boxHeight2),
                            style = Stroke(
                                width = 5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun emptyAnimations():ArrayList<ArrayList<Animatable<Float,AnimationVector1D>>>{
    val arrayList = arrayListOf<ArrayList<Animatable<Float,AnimationVector1D>>>()
    for(i in 0..2){
        arrayList.add(arrayListOf())
        for(j in 0..2){
            arrayList[i].add(Animatable(0f))
        }
    }
    return arrayList
}

private fun emptyGame():Array<CharArray>{

    return arrayOf(
        charArrayOf('E','E','E'),
        charArrayOf('E','E','E'),
        charArrayOf('E','E','E'),
    )
}
fun getTapIndexes(tapOffset:Offset, gameSize:Size):Array<Int>{

    val thirdX = gameSize.width / 3
    val thirdY = gameSize.height / 3
    val tapX = tapOffset.x
    val tapY = tapOffset.y
    val i = when{
        (tapY <= thirdY) -> 0
        (tapY <= thirdY*2) -> 1
        (thirdY*2 < tapY) -> 2
        else -> 0
    }
    val j = when{
        (tapX <= thirdX) -> 0
        (tapX <= thirdX*2) -> 1
        (thirdX*2 < tapX) -> 2
        else -> 0
    }
    //val res = j * 3 + i
    val res = arrayOf<Int>(i,j)
    Log.i("AAA","[i][j] = [$i][$j]")
    return res
}
fun updateGameState(gameState:Array<CharArray>,i:Int,j:Int,symbol: Char):Array<CharArray>{
    val gameCopy = gameState.copyOf()
    gameCopy[i][j] = symbol
    return gameCopy
}

private fun CoroutineScope.animateFloatToOne(animatable:Animatable<Float,AnimationVector1D>){
    launch{
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500
            )
        )
    }
}

fun isGameOver(gameState: Array<CharArray>):Boolean{
    //Vertical Win
    for(i in 0..2){
        if(gameState[0][i] == gameState[1][i] &&
            gameState[1][i] == gameState[2][i]){
            if(gameState[0][i] != 'E'){
                return true
            }
        }
    }
    //Horizontal Win
    for(i in 0..2){
        if(gameState[i][0] == gameState[i][1] &&
            gameState[i][1] == gameState[i][2]){
            if(gameState[i][0] != 'E'){
                return true
            }
        }
    }
    //Diagonal Win
    if((gameState[0][0] == gameState[1][1] && gameState[1][1] == gameState[2][2]) ||
        (gameState[0][2] == gameState[1][1] && gameState[1][1] == gameState[2][0])){
        if(gameState[1][1] != 'E'){
            return true
        }
    }
    return false
}
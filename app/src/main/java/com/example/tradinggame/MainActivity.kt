package com.example.tradinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tradinggame.ui.theme.TradingGameTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TradingGameTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TradingGameApp()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TradingGameApp() {
    TradingGameRuntime(modifier = Modifier
        .fillMaxSize()
        .wrapContentSize(Alignment.Center)
    )
}

@Composable
fun TradingGameRuntime(modifier: Modifier = Modifier) {
    val iterations = 15 //35
    val timeLow = 1
    val timeHigh = 5

    val game by remember { mutableStateOf(Game(iterations)) }
    var lastBotAction by remember { mutableStateOf("") }
    var lastPlayerAction by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var endgameMessage by remember { mutableStateOf("") }
    var iteration by remember { mutableStateOf(0) }
    LaunchedEffect(key1 = Unit){
        while (iteration < iterations && isActive) {
            val info = game.book.processQuote(game.bot.quote(iteration))
            lastBotAction = when (info.outcome) {
                "Lift" -> "%d Offer Lifted".format(info.price)
                "Hit" -> "%d Bid Hit".format(info.price)
                else -> "%s %d".format(info.side, info.price)
            }

            delay(1000)
            lastBotAction = ""

            val bookBuilder = StringBuilder()
            bookBuilder.append("----------------------\n")
            for (o in game.book.offers) {
                bookBuilder.append("         | %d \n".format(o))
            }
            for (b in game.book.bids.asReversed()) {
                if (b < 100) {
                    bookBuilder.append(" %d   |  \n".format(b))
                }
                else {
                    bookBuilder.append(" %d |  \n".format(b))
                }
            }
            book = bookBuilder.toString()

            delay((1000*Random.nextInt(timeLow, timeHigh)).toLong())

            iteration ++
        }
        lastBotAction = ""
        val res = StringBuilder()
        res.append("Position: %d\n".format(game.position))
        res.append("Trades: %s\n".format(game.trades.toString()))
        res.append("Settles @ %.2f\n".format(game.settlement))
        res.append("Bot Theo: %.2f\n".format(game.bot.theo))
        res.append("PnL: $ %.2f\n".format(game.settlement * game.position - game.trades.sum()))
        endgameMessage = res.toString()
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Iterations Left: %d".format(iterations-iteration))
        Text(text = book)
        Text(text = "Last Bot Action: %s".format(lastBotAction))
        Text(text = "Your Last Trade: %s".format(lastPlayerAction))
        Text(text = endgameMessage)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                if ((iteration < iterations) && game.book.offers.isNotEmpty()) {
                    lastPlayerAction = "Bought @ %d".format(game.book.bestOffer())
                    game.lift()
                }
            }) { Text(text = "Lift") }
            Button(onClick = {
                if ((iteration < iterations) && game.book.bids.isNotEmpty()) {
                    lastPlayerAction = "Sold @ %d".format(game.book.bestBid())
                    game.hit()
                }
            }) { Text(text = "Hit") }
        }
    }
}


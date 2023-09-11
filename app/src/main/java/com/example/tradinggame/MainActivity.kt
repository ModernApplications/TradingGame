package com.example.tradinggame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.tradinggame.ui.theme.TradingGameTheme
import kotlin.random.Random
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

const val ITERATIONS = 15 //35
const val MEAN_MIN = 100
const val MEAN_MAX = 250

const val STD_MIN = 5
const val STD_MAX = 50
const val STD_SETTLEMENT = 25

const val CROSS_PROB = 0.4

const val TIME_LOW = 1
const val TIME_HIGH = 5

const val Bid = 1
const val Offer = 0
const val Buy = 1
const val Sell = -1
val side_map = arrayOf("Offer", "Bid")

fun normal_sample(mean: Double, std: Double): Double {
    var prev_c = DoubleArray(100)
    prev_c[0] = 1.0

    fun c(k: Int): Double {
        if (prev_c[k] != 0.0) {
            return prev_c[k]
        } else {
            var res = 0.0
            for (m in 0 until k) {
                res += c(m) * c(k - 1 - m) / ((m + 1) * (2 * m + 1))
            }
            prev_c[k] = res
            return res
        }
    }

    val x = Random.nextDouble() * 2 - 1
    var res = 0.0
    for (k in 0..99) {
        res += c(k) / (2 * k + 1) * (sqrt(PI) / 2 * x).pow(2*k+1)
    }
    return mean + res * sqrt(2.0) * std
}

class Quote(val price: Int, val side: Int) {
}

class Book {
    val bids = mutableListOf<Int>()
    val offers = mutableListOf<Int>()

    fun best_offer(): Int {
        val res = offers.minOrNull()
        return if (res != null) {
            res
        } else {
            2000000000
        }
    }

    fun best_bid(): Int {
        val res = bids.maxOrNull()
        return if (res != null) {
            res
        } else {
            -2000000000
        }
    }

    fun display(): String {
        bids.sort()
        bids.reverse()
        offers.sort()
        offers.reverse()

        val res = StringBuilder()
        res.append("----------------------\n")
        for (o in offers) {
            res.append("         | %d \n".format(o))
        }
        for (b in bids) {
            res.append(" %d |  \n".format(b))
        }
        return res.toString()
    }

    fun clean_book() {
        if (offers.size > 5) {
            offers.remove(offers.max())
        }
        if (bids.size > 5) {
            bids.remove(bids.min())
        }
    }

    fun append(quote: Quote) {
        if (quote.side == Bid) {
            bids.add(quote.price)
        } else {
            offers.add(quote.price)
        }
        clean_book()
    }

    fun process_quote(quote: Quote): IntArray {
        val lift = (quote.side == Bid && quote.price >= best_offer())
        val hit = (quote.side == Offer && quote.price <= best_bid())
        val info = IntArray(2)
        if (lift) {
            info[0] = 0
            info[1] = best_offer()
            offers.remove(best_offer())
        } else if (hit) {
            info[0] = 1
            info[1] = best_bid()
            bids.remove(best_bid())
        } else {
            info[0] = 2
            info[1] = quote.price
            append(quote)
        }
        return info
    }
}

class Bot {
    val theo = Random.nextDouble(MEAN_MIN.toDouble(), MEAN_MAX.toDouble())

    fun calc_var(i: Int): Double {
        return STD_MAX - (i * (STD_MAX - STD_MIN)).toDouble() / ITERATIONS
    }

    fun quote(i: Int): Quote {
        val price = normal_sample(theo, calc_var(i))
        val cross = Random.nextDouble() < CROSS_PROB

        val side = if ((price < theo) == (cross)) {
            Offer
        } else {
            Bid
        }
        return Quote(price.toInt(), side)
    }
}

class Game {
    val bot = Bot()
    val book = Book()
    var position = 0
    val settlement = normal_sample(bot.theo, STD_SETTLEMENT.toDouble())
    val trades = mutableListOf<Int>()

    fun process_action(action: Char) {
        if (action == 'h') {
            execute(Sell, book.best_bid())
        } else if (action == 'l') {
            execute(Buy, book.best_offer())
        }
    }

    fun execute(direction: Int, price: Int) {
        position += direction
        trades.add(direction * price)
        if (direction > 0) {
            book.offers.remove(book.best_offer())
        } else {
            book.bids.remove(book.best_bid())
        }
    }
}

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
    val game by remember { mutableStateOf(Game()) }
    var last_event by remember { mutableStateOf("") }
    var player_action by remember { mutableStateOf("") }
    var last_action by remember { mutableStateOf("") }
    var book by remember { mutableStateOf("") }
    var res_string by remember { mutableStateOf("") }
    LaunchedEffect(key1 = Unit){
        var i = 0
        while (i in 0 until ITERATIONS && isActive) {
            val q = game.bot.quote(i)
            val info = game.book.process_quote(q)
            if (info[0] == 0) {
                last_event = "%d Offer Lifted".format(info[1])
            } else if (info[0] == 1) {
                last_event = "%d Bid Dropped".format(info[1])
            } else {
                last_event = "%s %d".format(side_map[q.side], q.price)
            }

            var iter = 0
            while (player_action == "" && iter < 1000) {
                delay(1)
                iter += 1
            }
            last_event = ""
            if (
                player_action == ""
                || (player_action == "h" && game.book.bids.isEmpty())
                || (player_action == "l" && game.book.offers.isEmpty())
            ) {
                last_action = "No Action"
            } else {
                if (player_action == "h") {
                    last_action = "Sold @ %d".format(game.book.best_bid())
                } else if (player_action == "l") {
                    last_action = "Bought @ %d".format(game.book.best_offer())
                }
                game.process_action(player_action.toCharArray()[0])
            }
            player_action = ""

            book = game.book.display()
            val variable_speed = Random.nextInt(TIME_LOW, TIME_HIGH)

            iter = 0
            while (player_action == "" && iter < 1000 * variable_speed) {
                delay(1)
                iter += 1
            }
            if (
                player_action == ""
                || (player_action == "h" && game.book.bids.isEmpty())
                || (player_action == "l" && game.book.offers.isEmpty())
            ) {
                last_action = "No Action"
            } else {
                if (player_action == "h") {
                    last_action = "Sold @ %d".format(game.book.best_bid())
                } else if (player_action == "l") {
                    last_action = "Bought @ %d".format(game.book.best_offer())
                }
                game.process_action(player_action.toCharArray()[0])
            }
            player_action = ""
            i += 1
        }
        last_event = ""
        val res = StringBuilder()
        res.append("Position: %d\n".format(game.position))
        res.append("Trades: %s\n".format(game.trades.toString()))
        res.append("Settles @ %.2f\n".format(game.settlement))
        res.append("Bot Theo: %.2f\n".format(game.bot.theo))
        res.append("PnL: $ %.2f\n".format(game.settlement * game.position - game.trades.sum()))
        res_string = res.toString()
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = book)
        Text(text = "Last Bot Action: %s".format(last_event))
        Text(text = "Your Last Trade: %s".format(last_action))
        Text(text = res_string)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { player_action = "l" },
            ) {
                Text(text = "Lift")
            }
            Button(
                onClick = { player_action = "h" },
            ) {
                Text(text = "Hit")
            }
        }

    }
}

//@Composable
//fun TradingGameRuntime(modifier: Modifier = Modifier) {
//    var result by remember { mutableStateOf( 1) }
//    var changer by remember { mutableStateOf( 1) }
//    LaunchedEffect(key1 = Unit){
//        while(isActive){
//            changer += 1
//            delay(1000)
//        }
//    }
//    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
//        Text(text = result.toString())
//        Text(text = changer.toString())
//        Button(
//            onClick = { result += 1 },
//        ) {
//            Text(text = "My button")
//        }
//    }
//}

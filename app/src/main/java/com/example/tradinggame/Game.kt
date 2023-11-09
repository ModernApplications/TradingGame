package com.example.tradinggame

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

private fun normalSample(mean: Double, std: Double): Double {
    val prevC = DoubleArray(100)
    prevC[0] = 1.0

    fun c(k: Int): Double {
        return if (prevC[k] != 0.0) {
            prevC[k]
        } else {
            var res = 0.0
            for (m in 0 until k) {
                res += c(m) * c(k - 1 - m) / ((m + 1) * (2 * m + 1))
            }
            prevC[k] = res
            res
        }
    }

    val x = Random.nextDouble() * 2 - 1
    var res = 0.0
    for (k in 0..99) {
        res += c(k) / (2 * k + 1) * (sqrt(PI) / 2 * x).pow(2*k+1)
    }
    return mean + res * sqrt(2.0) * std
}

class Game(iterations: Int) {
    data class Quote(val price: Int, val side: String)

    class Bot(private val iterations: Int) {
        companion object {
            const val MEAN_MIN = 100
            const val MEAN_MAX = 250
            const val STD_MIN = 5
            const val STD_MAX = 50
            const val CROSS_PROB = 0.4
        }

        val theo = Random.nextDouble(MEAN_MIN.toDouble(), MEAN_MAX.toDouble())

        private fun calcVar(i: Int): Double {
            return STD_MAX - (i * (STD_MAX - STD_MIN)).toDouble() / iterations
        }

        fun quote(i: Int): Quote {
            val price = normalSample(theo, calcVar(i))
            val cross = Random.nextDouble() < CROSS_PROB

            val side = if ((price < theo) == cross) {
                "Offer"
            } else {
                "Bid"
            }
            return Quote(price.toInt(), side)
        }
    }

    class Book {
        data class ProcessCallback(val outcome: String, val price: Int, val side: String?)

        val bids = mutableListOf<Int>()
        val offers = mutableListOf<Int>()

        fun bestOffer(): Int {
            return if (offers.isEmpty()) {
                2000000000
            } else {
                offers.last()
            }
        }

        fun bestBid(): Int {
            return if (bids.isEmpty()) {
                -2000000000
            } else {
                bids.last()
            }
        }

        fun lift(): Int {
            return offers.removeLast()
        }

        fun hit(): Int {
            return bids.removeLast()
        }

        private fun append(quote: Quote) {
            var i = 0
            val orders = if (quote.side == "Bid") {
                while (i < bids.size && quote.price > bids[i]) {
                    i ++
                }
                bids
            } else {
                while (i < offers.size && quote.price < offers[i]) {
                    i ++
                }
                offers
            }
            if (i != 0 && orders.size == 5) {
                orders.removeAt(0)
                i--
            }
            if (i != 0 || orders.size != 5) {
                if (i < orders.size) {
                    orders.add(i, quote.price)
                } else {
                    orders.add(quote.price)
                }
            }
        }

        fun processQuote(quote: Quote): ProcessCallback {
            return if (quote.side == "Bid" && quote.price >= bestOffer()) {
                ProcessCallback("Lift", lift(), null)
            } else if (quote.side == "Offer" && quote.price <= bestBid()) {
                ProcessCallback("Hit", hit(), null)
            } else {
                append(quote)
                ProcessCallback("Quote", quote.price, quote.side)
            }
        }
    }

    companion object {
        const val STD_SETTLEMENT = 25
    }

    val bot = Bot(iterations)
    val book = Book()
    var position = 0
    val settlement = normalSample(bot.theo, STD_SETTLEMENT.toDouble())
    val trades = mutableListOf<Int>()

    fun hit() {
        position --
        trades.add(-1 * book.bestBid())
        book.hit()
    }

    fun lift() {
        position ++
        trades.add(book.bestOffer())
        book.lift()
    }
}
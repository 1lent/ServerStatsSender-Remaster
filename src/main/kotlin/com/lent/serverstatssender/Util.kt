package com.lent.serverstatssender

import me.lucko.spark.api.SparkProvider
import me.lucko.spark.api.statistic.StatisticWindow
import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo
import me.lucko.spark.api.statistic.types.DoubleStatistic
import me.lucko.spark.api.statistic.types.GenericStatistic
import net.dv8tion.jda.api.EmbedBuilder
import org.bukkit.Bukkit
import java.awt.Color
import java.text.DecimalFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val spark = SparkProvider.get()

fun getEmbed(config: Config) = EmbedBuilder()
    .setTitle(config.embedTitle)
    .setColor(Color.BLACK)
    .addField("Statistics:", infoField, false)
    .apply {
        if (config.imageURL.isNotEmpty()) setImage(config.imageURL)
    }
    .build()

fun getEmbedTPS(config: Config) = EmbedBuilder()
    .setTitle(config.embedTitle)
    .setColor(Color.RED)
    .addField("WARNING:", warnInfo, false)
    .apply {
        if (config.imageURL.isNotEmpty()) setImage(config.imageURL)
    }
    .build()

private val decimalFormatter = DecimalFormat.getNumberInstance().apply {
    maximumFractionDigits = 2
}

val warnInfo: String get() {
    val tps: DoubleStatistic<TicksPerSecond>? = spark.tps()
    val tpsLastMin = tps?.poll(TicksPerSecond.MINUTES_1)?.roundToInt() ?: 0
    return """
        TPS LOW:
        > $tpsLastMin
    """.trimIndent()
}

val infoField: String get() {
    val tps: DoubleStatistic<TicksPerSecond>? = spark.tps()
    val tpsLast10Secs = tps?.poll(TicksPerSecond.SECONDS_10)?.roundToInt() ?: 0
    val tpsLast5Mins = tps?.poll(TicksPerSecond.MINUTES_5)?.roundToInt() ?: 0
    val tpsLastMin = tps?.poll(TicksPerSecond.MINUTES_1)?.roundToInt() ?: 0

    val cpuUsage: DoubleStatistic<StatisticWindow.CpuUsage> = spark.cpuSystem()
    val usageLastMin = cpuUsage.poll(StatisticWindow.CpuUsage.MINUTES_1)
    val cpuShit = usageLastMin * 100

    val mspt: GenericStatistic<DoubleAverageInfo, StatisticWindow.MillisPerTick>? = spark.mspt()
    val msptpolled = mspt?.poll(StatisticWindow.MillisPerTick.MINUTES_1)
    val msptMean = msptpolled?.percentile95th()?.roundToInt() ?: 0
    val mspt95Percentile = msptpolled?.percentile95th()?.roundToInt() ?: 0
    val playercount = Bukkit.getOnlinePlayers().size
    val Runtime: Runtime = Runtime.getRuntime()
    val memUsed = (Runtime.totalMemory() - Runtime.freeMemory()) / 1048576 //Converting

    return """
        TPS (last 10s, 5m): 
        > $tpsLast10Secs, $tpsLast5Mins
                
        Mspt (mean usage, 95th percentile):
        > $msptMean, $mspt95Percentile
        
        CPU Usage Last Min: ${decimalFormatter.format(cpuShit)}%
        Playercount: $playercount
        MemoryUsage: $memUsed Mb
    """.trimIndent()
}

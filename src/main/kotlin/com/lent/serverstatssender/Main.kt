package com.lent.serverstatssender

import me.lucko.spark.api.statistic.StatisticWindow.TicksPerSecond
import me.lucko.spark.api.statistic.types.DoubleStatistic
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.roundToInt


class Main : JavaPlugin(), CommandExecutor, Listener {
    val tps: DoubleStatistic<TicksPerSecond>? = spark.tps()
    val tpsLast10Secs = tps?.poll(TicksPerSecond.SECONDS_10)?.roundToInt() ?: 0

    lateinit var jda: JDA private set
    lateinit var statsConfig: Config private set

    val isBotEnabled get() = ::jda.isInitialized

    private var scheduleThingy: BukkitTask? = null

    companion object {
        lateinit var plugin: Main private set

        fun scheduleTimer(intervalTicks: Long, delayTicks: Int = 0, runnable: () -> Unit) =
            Bukkit.getScheduler().runTaskTimer(plugin, Runnable { runnable.invoke() }, delayTicks.toLong(), intervalTicks.toLong())

    }
        //runnable invoke means uses code () -> Unit

    override fun onEnable() {
        plugin = this
        saveDefaultConfig()
        statsConfig = Config.load(this)


        getCommand("sssreload")?.setExecutor(this)
        getCommand("sssactivate")?.setExecutor(ActivateToken())

        if (statsConfig.token.isNotEmpty() && statsConfig.chanid.isNotEmpty()) {
            activateBot(statsConfig.token)
        } else {
            println("There is no token, please put bot token in config or use the command /sssactivate <token> <channelId>")
        }
        scheduleMethod()
        Bukkit.getPluginManager().registerEvents(this, this)
        logger.info("Server Stats Sender v1.0.3 online!")
    }


    fun activateBot(token: String) {
        jda = JDABuilder.createDefault(token)
            .setActivity(Activity.watching("your server stats"))
            .addEventListeners(DiscordListener(this))
            .build()
    }

    fun updateConfig(changes: FileConfiguration.() -> Unit) {
        changes.invoke(config)
        saveConfig()
        statsConfig = Config.load(this)
    }

    @EventHandler
    fun onjoin(event: PlayerJoinEvent) {
        if (!event.player.hasPermission("sss.access") || !event.player.hasPermission("sss.token")) return
        if (isBotEnabled) return
        event.player.sendMessage("${ChatColor.RED}${ChatColor.BOLD}Attention! ${ChatColor.WHITE}" + "There is no token in Server Stats Sender config")
    }

    private fun scheduleMethod() {
        scheduleThingy = scheduleTimer(statsConfig.timeIntervalTicks.toLong()) {
            if (!isBotEnabled || !statsConfig.repeat) return@scheduleTimer
            val channel = jda.getTextChannelById(statsConfig.chanid) ?: return@scheduleTimer
            if (statsConfig.embed) channel.sendMessageEmbeds(getEmbed(statsConfig)).queue()
            else channel.sendMessage(infoField).queue()
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("sss.access")) return true
        scheduleThingy?.cancel()
        reloadConfig()
        statsConfig = Config.load(this)
        scheduleMethod()
        sender.sendMessage("${ChatColor.GREEN}${ChatColor.BOLD}Success! ${ChatColor.WHITE}Server Stats Bot config reloaded")
        return true
    }
    fun onTpsLow() {
        if (tpsLast10Secs <= 10) {
        val channel = jda.getTextChannelById(statsConfig.chanid)
        channel?.sendMessageEmbeds(getEmbedTPS(statsConfig))?.queue()
    }
    }

}

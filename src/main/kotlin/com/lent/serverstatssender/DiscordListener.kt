package com.lent.serverstatssender

import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands

class DiscordListener(val plugin: Main): ListenerAdapter() {
    val config get() = plugin.statsConfig

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        super.onSlashCommandInteraction(event)
        if (event.name != config.cmdNameForDiscord) return

        if (config.embedCmd) {
            event.channel.sendMessageEmbeds(getEmbed(config)).queue()
        } else {
            event.reply(infoField).queue()
        }
    }

    override fun onGuildReady(event: GuildReadyEvent) {
        val cmdName = Main.plugin.config.getString("cmdNameForDiscord")
        val guild = event.guild
        guild.updateCommands()
            .addCommands(cmdName?.let { Commands.slash(it, "Shows the server stats.") })
            .queue()
    }



}
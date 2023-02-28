package com.discordsrv.apitest;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class Plugin extends JavaPlugin implements Listener, SlashCommandProvider, AutoCloseable {

    private final DiscordSRVListener discordsrvListener = new DiscordSRVListener(this);
    private Economy econ = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        DiscordSRV.api.subscribe(discordsrvListener);
        registerEcon();
        saveDefaultConfig();
    }
    @Override
    public void onDisable() {
        DiscordSRV.api.unsubscribe(discordsrvListener);
    }

    public boolean registerEcon() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().severe("Vault not loaded!");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger().severe("Economy service registration is null!");
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("stats", "See a players stats")
                        .addOption(OptionType.STRING, "player-name", "Name of player u want to see stats of", true)
                ),

                new PluginSlashCommand(this, new CommandData("balance", "Check how broke someone is")
                        .addOption(OptionType.STRING, "player-name", "Name of player u want to see balance of", true)),

                new PluginSlashCommand(this, new CommandData("player-list", "See a list of currently online players")),
                new PluginSlashCommand(this, new CommandData("server-info", "See basic information about the server"))
                ));
    }

    @SlashCommand(path = "balance")
    public void balanceCommand(SlashCommandEvent event) {
        if (!Objects.equals(this.getConfig().get("balance-enabled"), true)) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString())) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        if (econ == null) {event.reply("VAULT ECONOMY UNABLE TO LOAD").queue(); return;}
        String playerName = String.valueOf(event.getOption("player-name"));
        double bal = econ.getBalance(playerName);
        event.reply(playerName + " 's balance is " + bal).queue();
    }
    @SlashCommand(path = "server-info")
    public void serverinfo(SlashCommandEvent event) throws IOException {
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString())) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        if (!Objects.equals(this.getConfig().get("serverinfo-enabled"), true)) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
       if (this.getConfig().get("ip").toString().equalsIgnoreCase("0.0.0.0")) {
 //you get the IP as a String
           EmbedBuilder eb = new EmbedBuilder();
           HashSet<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
           eb.addField("Info", "IP: " + "Change this in Config.yml" + "\n" + "Player Count: " + set.size() + "/" + Bukkit.getMaxPlayers() + "\n" + "Version: " + Bukkit.getServer().getVersion(), false);
           eb.setColor(Color.RED);
           eb.setTitle("Server Info", null);
           event.replyEmbeds(eb.build()).setEphemeral(true).queue();
       }
       else {
        EmbedBuilder eb = new EmbedBuilder();
        HashSet<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
        eb.addField("Info", "IP: " + this.getConfig().get("ip") + "\n" + "Player Count: " + set.size() + "/" + Bukkit.getMaxPlayers() + "\n" + "Version: " + Bukkit.getServer().getVersion(), false);
        eb.setColor(Color.RED);
        eb.setTitle("Server Info", null);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
}}
    @SlashCommand(path = "player-list")
    public void playerlist(SlashCommandEvent event) {
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString())) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        if (!Objects.equals(this.getConfig().get("playerlist-enabled"), true)) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
        EmbedBuilder eb = new EmbedBuilder();
        HashSet<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
        String playerlist = set.stream().map(Player::getName).collect(Collectors.joining(", "));
        eb.setTitle("Player List", null);
        eb.addField("", "Online Players: " + playerlist, false);
        eb.setColor(Color.RED);
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
@SlashCommand(path = "stats")
    public void stats(SlashCommandEvent event) throws IOException {
    EmbedBuilder eb = new EmbedBuilder();
    OfflinePlayer displayName = Bukkit.getOfflinePlayer(event.getOption("player-name").getAsString());
    long playtime = 0;
    if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString())) {
        event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
    if (!Objects.equals(this.getConfig().get("stats-enabled"), true)) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
    int deaths = displayName.getStatistic(Statistic.DEATHS);
    int playerkills = displayName.getStatistic(Statistic.PLAYER_KILLS);
    int mobkills = displayName.getStatistic(Statistic.MOB_KILLS);
    playtime = displayName.getStatistic(Statistic.PLAY_ONE_MINUTE);
    long time = (playtime/20);
    long timeminutes = (time / 60);
    long timehours = (timeminutes / 60);
    long timedays = (timehours / 24);
    timeminutes-=timehours*60;
    timehours-=timedays*24;
    if (playtime <= 0) {event.reply( displayName.getName() + " hasn't joined this server").queue(); return;}
    eb.setTitle(displayName.getName(), null);
    eb.setColor(Color.RED);
    eb.addField("", "- **Playtime **- " + timedays +  " Days, " + timehours + " Hours, "  + timeminutes + " Minutes " + "\n" + "- **Deaths **- " + deaths + "\n" + "- **Mob Kills **- " + mobkills + "\n" + "- **Player Kills **- " + playerkills + "\n", false);
    eb.setImage("https://crafatar.com/avatars/" + displayName.getUniqueId());
    event.replyEmbeds(eb.build()).setEphemeral(true).queue();
//event.reply(displayName.getName() + " = " + timedays +  " Days, " + timehours + " Hours, "  + timeminutes + " Minutes " + "\n" + displayName.getName() + " has died " + deaths + " times" + "\n" + displayName.getName() + " has died " + mobkills + " monsters" + "\n" + displayName.getName() + " has killed " + playerkills + " people" + "\n").setEphemeral(true)
  //          .addFile(new File(displayName.getUniqueId() + ".png")).queue();
    }
    @Override
    public void close() throws Exception {

    }
}


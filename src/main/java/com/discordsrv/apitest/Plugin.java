package com.discordsrv.apitest;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import me.clip.placeholderapi.PlaceholderAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Plugin extends JavaPlugin implements Listener, SlashCommandProvider {

    private final DiscordSRVListener discordsrvListener = new DiscordSRVListener(this);
    private Economy econ = null;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        DiscordSRV.api.subscribe(discordsrvListener);
        saveDefaultConfig();
        for(int i = 1; ; i++) {
            String config = getConfig().getString("dev" + i + "-id");
            if (config == null) {
                break;
            }
            AuthList.put(getConfig().getString("dev" + i + "-id"), getConfig().getString("dev" + i + "-code"));
        }
    }
    @Override
    public void onDisable() {
        DiscordSRV.api.unsubscribe(discordsrvListener);
        Authenticated.clear();
    }
    HashMap<String, String> AuthList = new HashMap<>();
    HashSet<String> Authenticated = new HashSet<>();

    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("stats", "See a players stats")
                        .addOption(OptionType.STRING, "player-name", "Name of player u want to see stats of", true)),
                new PluginSlashCommand(this, new CommandData("auth", "Staff Authentication")
                        .addOption(OptionType.STRING, "code", "Auth Code", true)),
                new PluginSlashCommand(this, new CommandData("cmd", "Run commands after authenticating")
                        .addOption(OptionType.STRING, "command", "Command to run", true)),
                new PluginSlashCommand(this, new CommandData("player-list", "See a list of currently online players")),
                new PluginSlashCommand(this, new CommandData("server-info", "See basic information about the server")),
                new PluginSlashCommand(this, new CommandData("reload", "reloads the plugin"))
        ));
    }
    @SlashCommand(path = "auth")
    public void authCommand(SlashCommandEvent event) {
        if (AuthList.get(event.getUser().getId()).equals(event.getOption("code").getAsString())) {
            Authenticated.add(event.getUser().getId());
            event.reply("you have been authenticated till next server restart").setEphemeral(true).queue();
        }
        else event.reply("You are either not a authorized staff member or you have used the wrong code").setEphemeral(true).queue();
    }
    @SlashCommand(path = "cmd")
    public void commandRunner(SlashCommandEvent event) {
     if (Authenticated.contains(event.getUser().getId())) {
         new BukkitRunnable() {
             @Override
             public void run() {
                 commandDispatch(event.getOption("command").getAsString());
                 event.reply("Command Has Been Executed").setEphemeral(true).queue();
             }
         }.runTask(this);
     }
     else event.reply("you are not authenticated").setEphemeral(true).queue();
    }
    public void commandDispatch(String cmd) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
    @SlashCommand(path = "server-info")
    public void serverinfo(SlashCommandEvent event) throws IOException {
        List<String> serverInfo = getConfig().getStringList("server-info");

        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        if (!this.getConfig().getBoolean("serverinfo-enabled")) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
            if (!getConfig().getBoolean("placeholderapi-support")){
            EmbedBuilder eb = new EmbedBuilder();
            HashSet<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
            eb.addField("Info", "IP: " + this.getConfig().get("ip") + "\n" + "Player Count: " + set.size() + "/" + Bukkit.getMaxPlayers() + "\n" + "Version: " + Bukkit.getServer().getVersion(), false);
            eb.setColor(Color.RED);
            eb.setTitle("Server Info", null);

            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        }
            else if (getConfig().getBoolean("placeholderapi-support")) {
                EmbedBuilder eb = new EmbedBuilder();
                for (String unparsedText : serverInfo) {
                    String parsedPlaceholders = PlaceholderAPI.setPlaceholders(null, unparsedText);
                    eb.addField("", parsedPlaceholders, false); }

            eb.setColor(Color.RED);
            eb.setTitle("Server Info", null);
                eb.setFooter("SlashAdditions");
            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
    }
    @SlashCommand(path = "reload")
    public void reloadCommand(SlashCommandEvent event){
        if (Authenticated.contains(event.getUser().getId())) {
            reloadConfig();
            event.reply("Plugin has been reloaded").setEphemeral(true).queue();
        }
        else event.reply("you are not authenticated").setEphemeral(true).queue();
    }
    @SlashCommand(path = "player-list")
    public void playerlist(SlashCommandEvent event) {
        if (!this.getConfig().getBoolean("playerlist-enabled")) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        EmbedBuilder eb = new EmbedBuilder();
        HashSet<Player> set = new HashSet<>(Bukkit.getOnlinePlayers());
        String playerlist = set.stream().map(Player::getName).collect(Collectors.joining(", "));
        eb.setTitle("Player List", null);
        eb.addField("", "Online Players: " + playerlist, false);
        eb.setColor(Color.RED);
        eb.setFooter("SlashAdditions");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }
    @SlashCommand(path = "stats")
    public void stats(SlashCommandEvent event) throws IOException {
        EmbedBuilder eb = new EmbedBuilder();
        OfflinePlayer displayName = Bukkit.getOfflinePlayer(event.getOption("player-name").getAsString());
        long playtime = 0;
        if (!this.getConfig().getBoolean("stats-enabled")) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
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
        if (playtime <= 0) {event.reply( displayName.getName() + " hasn't joined this server before").setEphemeral(true).queue(); return;}
        eb.setTitle(displayName.getName(), null);
        eb.setColor(Color.RED);
            if (!getConfig().getBoolean("placeholderapi-support")) {
        eb.addField("", "- **Playtime **- " + timedays +  " Days, " + timehours + " Hours, "  + timeminutes + " Minutes " + "\n" + "- **Deaths **- " + deaths + "\n" + "- **Mob Kills **- " + mobkills + "\n" + "- **Player Kills **- " + playerkills + "\n", false);}
        if (getConfig().getBoolean("placeholderapi-support")) {
            List<String> playerStats = getConfig().getStringList("stats");
            for (String unparsedText : playerStats) {
                String parsedPlaceholders = PlaceholderAPI.setPlaceholders(displayName, unparsedText);
                eb.addField("", parsedPlaceholders, false); }
        if (getConfig().getBoolean("display-avatar")) { eb.setImage("https://crafatar.com/avatars/" + displayName.getUniqueId()); }
        eb.setFooter("SlashAdditions");
        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

}}
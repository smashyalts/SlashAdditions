package com.discordsrv.apitest;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.commands.PluginSlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommand;
import github.scarsz.discordsrv.api.commands.SlashCommandProvider;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.SlashCommandEvent;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.OptionType;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.CommandData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.awt.*;
import java.io.IOException;
import java.util.*;
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
        if (getConfig().getString("dev1-id") != null){ AuthList.put(getConfig().getString("dev1-id"), getConfig().getString("dev1-code"));}
        if (getConfig().getString("dev2-id") != null){ AuthList.put(getConfig().getString("dev2-id"), getConfig().getString("dev2-code"));}
        if (getConfig().getString("dev3-id") != null){ AuthList.put(getConfig().getString("dev3-id"), getConfig().getString("dev3-code"));}
        if (getConfig().getString("dev4-id") != null){ AuthList.put(getConfig().getString("dev4-id"), getConfig().getString("dev4-code"));}
        if (getConfig().getString("dev5-id") != null){ AuthList.put(getConfig().getString("dev5-id"), getConfig().getString("dev5-code"));}
    }
    @Override
    public void onDisable() {
        DiscordSRV.api.unsubscribe(discordsrvListener);
        Authenticated.clear();
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
    HashMap<String, String> AuthList = new HashMap<>();
    HashSet<String> Authenticated = new HashSet<>();


    @Override
    public Set<PluginSlashCommand> getSlashCommands() {
        return new HashSet<>(Arrays.asList(
                new PluginSlashCommand(this, new CommandData("stats", "See a players stats")
                        .addOption(OptionType.STRING, "player-name", "Name of player u want to see stats of", true)
                ),

                new PluginSlashCommand(this, new CommandData("balance", "Check someones balance")
                        .addOption(OptionType.STRING, "player-name", "Name of player u want to see balance of", true)),
                new PluginSlashCommand(this, new CommandData("auth", "Staff Authenticate")
                        .addOption(OptionType.STRING, "code", "Auth Code", true)),
                new PluginSlashCommand(this, new CommandData("cmd", "Run commands after authenticating")
                        .addOption(OptionType.STRING, "command", "Command to run", true)),
                new PluginSlashCommand(this, new CommandData("player-list", "See a list of currently online players")),
                new PluginSlashCommand(this, new CommandData("server-info", "See basic information about the server"))
        ));
    }
    @SlashCommand(path = "auth")
    public void authenticate(SlashCommandEvent event) {
        if (AuthList.get(event.getUser().getId()).equalsIgnoreCase(Objects.requireNonNull(event.getOption("code")).getAsString())) {
            Authenticated.add(event.getUser().getId());
            event.reply("you have been authenticated till next server restart");
        }
        else event.reply("You are either not a authorized staff member or you have inputted the wrong code");
    }
    @SlashCommand(path = "cmd")
    public void command(SlashCommandEvent event) {
        if (Authenticated.contains(event.getUser().getId())) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Objects.requireNonNull(event.getOption("command")).getAsString());
            event.reply("Command Has Been Ran");
        }
        else event.reply("You are not authenticated");
    }

    @SlashCommand(path = "balance")
    public void balanceCommand(SlashCommandEvent event) {
        if (!Objects.equals(this.getConfig().get("balance-enabled"), true)) {event.reply("This command has been disabled").setEphemeral(true).queue(); return;}
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
            event.reply("This command cannot be used in this channel").setEphemeral(true).queue(); return;}
        if (econ == null) {event.reply("VAULT ECONOMY UNABLE TO LOAD").queue(); return;}
        String playerName = String.valueOf(event.getOption("player-name"));
        double bal = econ.getBalance(playerName);
        event.reply(playerName + " 's balance is " + bal).queue();
    }
    @SlashCommand(path = "server-info")
    public void serverinfo(SlashCommandEvent event) throws IOException {
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
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
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
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
        if (!event.getMessageChannel().getId().equalsIgnoreCase(Objects.requireNonNull(this.getConfig().get("main-channel")).toString()) && !this.getConfig().get("only-mainchannel").equals(false)) {
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
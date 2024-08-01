package net.flectone.chat;

import lombok.Getter;
import lombok.Setter;
import net.flectone.chat.database.sqlite.Database;
import net.flectone.chat.manager.FActionManager;
import net.flectone.chat.manager.FModuleManager;
import net.flectone.chat.manager.FPlayerManager;
import net.flectone.chat.manager.FileManager;
import net.flectone.chat.model.metric.Metrics;
import net.flectone.chat.module.FModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public final class FlectoneChat extends JavaPlugin {

    @Getter
    private static FlectoneChat plugin;

    private FModuleManager moduleManager;
    private FActionManager actionManager;
    private FileManager fileManager;
    private FPlayerManager playerManager;
    private Database database;

    @Getter
    private Scoreboard scoreBoard;

    @Override
    public void onEnable() {
        plugin = this;

        this.playerManager = new FPlayerManager();

        this.fileManager = new FileManager();
        this.fileManager.init();

        setScoreBoard();

        this.database = new Database(this);
        this.database.loadOfflinePlayersToDatabase();

        this.actionManager = new FActionManager();
        this.moduleManager = new FModuleManager();
        this.moduleManager.init();

        playerManager.loadOfflinePlayers();
        playerManager.loadOnlinePlayers();
        playerManager.loadTabCompleteData();

        /*if (fileManager.getConfig().getBoolean("plugin.bStats.enable")) {
            Metrics bStats = new Metrics(this, 20290);
            bStats.addCustomChart(new Metrics.SimplePie("plugin_language", () ->
                    fileManager.getConfig().getString("plugin.language")));
            bStats.addCustomChart(new Metrics.AdvancedPie("modules", () -> {
                Map<String, Integer> map = new HashMap<>();
                moduleManager.getModules().forEach(fModule -> map.put(fModule.getName(), 1));
                return map;
            }));
        }*/
    }

    public void setScoreBoard() {
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) return;

        scoreBoard = fileManager.getConfig().getBoolean("plugin.scoreboard.custom")
                ? scoreboardManager.getNewScoreboard()
                : scoreboardManager.getMainScoreboard();
    }

    @Override
    public void onDisable() {


        playerManager.terminateAll();

        actionManager.clearAll();
        database.getExecutor().close();
        database.disconnect();
    }

    public static void info(String message) {
        getPlugin().getLogger().info(message);
    }

    public static void warning(String message) {
        getPlugin().getLogger().warning(message);
    }
}

package com.enea.antibookban;

import com.enea.antibookban.command.AbbCommand;
import com.enea.antibookban.command.FixAllCommand;
import com.enea.antibookban.command.ScanCommand;
import com.enea.antibookban.config.PluginConfig;
import com.enea.antibookban.listener.BookListener;
import com.enea.antibookban.listener.JoinScanListener;
import com.enea.antibookban.util.ShulkerUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class AntiBookBan extends JavaPlugin {

    private static AntiBookBan instance;
    private PluginConfig pluginConfig;

    public static AntiBookBan getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        pluginConfig = new PluginConfig(this);

        ScanCommand scanCommand = new ScanCommand(this, pluginConfig);
        getCommand("scan").setExecutor(scanCommand);

        FixAllCommand fixAllCommand = new FixAllCommand(this, pluginConfig);
        getCommand("fixall").setExecutor(fixAllCommand);

        getCommand("abb").setExecutor(new AbbCommand(pluginConfig));

        getServer().getPluginManager().registerEvents(new BookListener(pluginConfig), this);

        if (pluginConfig.isScanOnJoin()) {
            getServer().getPluginManager().registerEvents(new JoinScanListener(this, pluginConfig), this);
        }

        getLogger().info("AntiBookBan enabled. Shulker limit: "
                + ShulkerUtil.humanSize(pluginConfig.getMaxShulkerSize())
                + ", max " + pluginConfig.getMaxBookPages() + " book pages"
                + (pluginConfig.isAsciiOnly() ? " (ASCII only)" : ""));
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}

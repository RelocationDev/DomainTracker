package me.rickylafleur.domaintracker;

import com.maxmind.geoip2.DatabaseReader;
import lombok.Getter;
import me.lucko.helper.Schedulers;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import me.lucko.helper.plugin.ap.Plugin;
import me.rickylafleur.domaintracker.commands.JoinsCommand;
import me.rickylafleur.domaintracker.listeners.JoinListener;
import me.rickylafleur.domaintracker.storage.Database;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

@Getter
@Plugin(
        name = "DomainTracker",
        version = "1.1",
        authors = {"Ricky Lafleur, Relocation"},
        apiVersion = "1.13"
)
public final class DomainTracker extends ExtendedJavaPlugin {

    private static DomainTracker instance;

    private final FastDateFormat format = FastDateFormat.getInstance("MM-dd-yyyy");
    private final Database database = new Database(this);
    private final File databaseFile = new File(this.getDataFolder(), "GeoIP2-Country.mmdb");
    private DatabaseReader maxMindReader;

    @Override
    protected void enable() {
        DomainTracker.instance = this;

        this.saveDefaultConfig();

        try {
            this.database.connect();
        } catch (final SQLException exception) {
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }

        Bukkit.getLogger().log(Level.INFO, "Database connected successfully!");
        Schedulers.async().run(this::checkDatabase);

        // Commands
        bindModule(new JoinsCommand(this));

        // Listeners
        bindModule(new JoinListener(this));
    }

    @Override
    protected void disable() {
        this.database.disconnect();
    }

    private void checkDatabase() {

        if (this.databaseFile.exists()) {
            try {
                this.maxMindReader = new DatabaseReader.Builder(this.databaseFile).build();
            } catch (final IOException e) {
                e.printStackTrace();
            }

            return;
        }

        if (this.getConfig().getBoolean("database.download-if-missing", true)) {
            this.downloadDatabase();
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "Cannot find GeoIP database.");
            return;
        }

        if (this.getConfig().getBoolean("database.update.enabled", true)) {
            final long diff = new Date().getTime() - this.databaseFile.lastModified();
            if (diff / 24 / 3600 / 1000 > this.getConfig().getLong("database.update.every-x-days", 30)) {
                this.downloadDatabase();
            }
        }

        try {
            this.maxMindReader = new DatabaseReader.Builder(this.databaseFile).build();
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadDatabase() {
        try {

            String url = this.getConfig().getString("database.download-url", null);

            if (url == null || url.isEmpty()) {
                Bukkit.getLogger().log(Level.SEVERE, "GeoIP url empty.");
                return;
            }

            final String licenseKey = this.getConfig().getString("database.license-key", "");

            if (licenseKey.isEmpty()) {
                Bukkit.getLogger().log(Level.SEVERE, "GeoIP license missing.");
                return;
            }

            url = url.replace("{LICENSEKEY}", licenseKey);
            Bukkit.getLogger().log(Level.INFO, "Downloading GeoIP database.");

            final URL downloadUrl = new URL(url);
            final URLConnection connection = downloadUrl.openConnection();

            connection.setConnectTimeout(10000);
            connection.connect();

            InputStream input = connection.getInputStream();

            final OutputStream output = new FileOutputStream(this.databaseFile);
            final byte[] buffer = new byte[2048];

            if (url.contains("gz")) {
                input = new GZIPInputStream(input);

                if (url.contains("tar.gz")) {

                    final TarInputStream tarInputStream = new TarInputStream(input);
                    TarEntry entry;

                    while ((entry = tarInputStream.getNextEntry()) != null) {
                        if (entry.isDirectory()) {
                            continue;
                        }

                        if (entry.getName().substring(entry.getName().length() - 5).equalsIgnoreCase(".mmdb")) {
                            input = tarInputStream;
                            break;
                        }
                    }
                }
            }

            int length = input.read(buffer);
            while (length >= 0) {
                output.write(buffer, 0, length);
                length = input.read(buffer);
            }

            output.close();
            input.close();
        } catch (final IOException e) {
            e.printStackTrace();
        }

    }
}

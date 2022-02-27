package me.rickylafleur.domaintracker.storage.impl;

import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import me.lucko.helper.sql.DatabaseCredentials;
import me.lucko.helper.sql.plugin.HelperSql;
import me.rickylafleur.domaintracker.DomainTracker;
import me.rickylafleur.domaintracker.storage.Database;
import me.rickylafleur.domaintracker.storage.data.JoinData;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author Ricky Lafleur
 */
public final class JoinDatabase implements Database {

    private final DomainTracker plugin;
    private HelperSql helperSql;

    public JoinDatabase(DomainTracker plugin) {
        this.plugin = plugin;
    }

    @Override
    public void connect() throws SQLException {
        this.helperSql = new HelperSql(DatabaseCredentials.fromConfig(plugin.getConfig().getConfigurationSection("mysql")));
        this.createTable();
    }

    @Override
    public void disconnect() {
        if (this.isConnected()) {
            try {
                this.helperSql.getConnection().close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isConnected() {
        try {
            this.helperSql.getConnection();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void addData(
            final String date,
            final String uuid,
            final String domain,
            final String country) {
        
        if (this.playerExists(UUID.fromString(uuid))) {
            return;
        }

        this.helperSql.executeAsync("INSERT INTO domain_tracker (DATE,UUID,DOMAIN,COUNTRY) VALUES (?,?,?,?);", ps -> {
            ps.setString(1, uuid);
            ps.setString(2, date);
            ps.setString(3, domain);
            ps.setString(4, country);
        });
    }

    public Set<JoinData> getJoinData() {
        return this.helperSql.queryAsync("SELECT * FROM domain_tracker;", rs -> {
            final Set<JoinData> joinDataSet = new HashSet<>();

            while (rs.next()) {
                joinDataSet.add(new JoinData(
                        UUID.fromString(rs.getString("UUID")),
                        rs.getString("DATE"),
                        rs.getString("DOMAIN"),
                        rs.getString("COUNTRY")
                ));
            }

            return joinDataSet;
        }).join().orElse(Collections.emptySet());
    }

    public Set<JoinData> getJoinData(final String date) {
        return this.helperSql.queryAsync("SELECT * FROM domain_tracker WHERE DATE = ?;", ps -> ps.setString(1, date), rs -> {
            final Set<JoinData> joinDataSet = new HashSet<>();

            while (rs.next()) {
                joinDataSet.add(new JoinData(
                        UUID.fromString(rs.getString("UUID")),
                        rs.getString("DATE"),
                        rs.getString("DOMAIN"),
                        rs.getString("COUNTRY")
                ));
            }
            return joinDataSet;
        }).join().orElse(Collections.emptySet());
    }

    public boolean playerExists(final UUID uuid) {
        return this.helperSql.queryAsync("SELECT * FROM domain_tracker WHERE `UUID` = ?;", ps
                -> ps.setString(1, uuid.toString()), ResultSet::next).join().orElse(false);
    }

    public String getCountryFromIp(final InetAddress address) {
        try {
            final CountryResponse response = this.plugin.getMaxMindReader().country(address);
            return response.getCountry().getName();
        } catch (final IOException | GeoIp2Exception e) {
            Bukkit.getLogger().log(Level.INFO, "IP not found in database defaulting to Unknown");
        }

        return "Unknown";
    }

    @Override
    public void createTable() {
        this.helperSql.executeAsync("CREATE TABLE IF NOT EXISTS `domain_tracker` (`DATE` char(10), `UUID` char(36), `DOMAIN` char(50), `COUNTRY` char(50));");
    }
}

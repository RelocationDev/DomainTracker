package me.rickylafleur.domaintracker.listeners;

import me.lucko.helper.Events;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import me.rickylafleur.domaintracker.DomainTracker;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.List;

/**
 * @author Ricky Lafleur
 */
public final class JoinListener implements TerminableModule {

    private final DomainTracker plugin;
    private final List<String> domains;

    public JoinListener(final DomainTracker plugin) {
        this.plugin = plugin;
        this.domains = plugin.getConfig().getStringList("domains");
    }

    @Override
    public void setup(@Nonnull final TerminableConsumer consumer) {
        Events.subscribe(PlayerLoginEvent.class)
                .filter(event -> event.getResult() == PlayerLoginEvent.Result.ALLOWED)
                .handler(event -> {
                    final Player player = event.getPlayer();
                    final String hostname = event.getHostname();

                    if (this.plugin.getConfig().getBoolean("only-count-unique", false) && player.hasPlayedBefore()) {
                        return;
                    }

                    if (this.plugin.getJoinDatabase().playerExists(player.getUniqueId())) {
                        return;
                    }

                    if (this.domains.contains(hostname)) {
                        this.plugin.getJoinDatabase().addData(
                                this.plugin.getFormat().format(new Date()),
                                player.getUniqueId().toString(),
                                hostname,
                                this.plugin.getJoinDatabase().getCountryFromIp(event.getAddress()));
                    }
                })
                .bindWith(consumer);
    }
}

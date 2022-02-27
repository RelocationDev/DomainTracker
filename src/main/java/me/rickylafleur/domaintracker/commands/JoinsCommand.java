package me.rickylafleur.domaintracker.commands;

import me.lucko.helper.Commands;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import me.rayzr522.jsonmessage.JSONMessage;
import me.rickylafleur.domaintracker.DomainTracker;
import me.rickylafleur.domaintracker.storage.data.JoinData;
import me.rickylafleur.domaintracker.utils.Text;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ricky Lafleur
 */
public final class JoinsCommand implements TerminableModule {

    private final DomainTracker plugin;
    private final List<String> displays, countries, domains;

    public JoinsCommand(final DomainTracker plugin) {
        this.plugin = plugin;
        this.displays = plugin.getConfig().getStringList("display");
        this.countries = plugin.getConfig().getStringList("countries");
        this.domains = plugin.getConfig().getStringList("domains");
    }

    @Override
    public void setup(@Nonnull final TerminableConsumer consumer) {
        Commands.create()
                .assertPlayer()
                .assertPermission("domaintracker.admin")
                .assertUsage("<MM-dd-yyyy>")
                .handler(command -> {

                    final Player player = command.sender();
                    String date = command.arg(0).parseOrFail(String.class);

                    if (date.equalsIgnoreCase("today")) {
                        date = this.plugin.getFormat().format(new Date());
                    }

                    final Set<JoinData> joinDataSet;

                    if (date.equalsIgnoreCase("all")) {
                        joinDataSet = this.plugin.getJoinDatabase().getJoinData();
                    } else {
                        joinDataSet = this.plugin.getJoinDatabase().getJoinData(date);
                    }

                    if (joinDataSet.isEmpty()) {
                        player.sendMessage(Text.colorize("&cInvalid date or no joins for that date."));
                        return;
                    }

                    int i = 0;
                    for (final String domain : this.domains) {

                        final List<JoinData> joins = joinDataSet
                                .stream()
                                .filter(joinData -> joinData.getDomain().equals(domain))
                                .sorted(Comparator.comparing(JoinData::getCountry))
                                .collect(Collectors.toList());

                        final List<String> countries = joins
                                .stream()
                                .map(JoinData::getCountry)
                                .collect(Collectors.toList());

                        final Map<String, Integer> countryJoins = new HashMap<>();

                        for (final String country : this.countries) {
                            int frequency = Collections.frequency(countries, country);

                            if (frequency <= 0) {
                                return;
                            }

                            countryJoins.put(country, frequency);
                        }

                        final JSONMessage display = JSONMessage
                                .create(Text.colorize("&a" + this.displays.get(i) + " &8- &7" + joins.size() + " joins"))
                                .tooltip(countryJoins.entrySet()
                                        .stream()
                                        .map(join -> Text.colorize("&a&l" + join.getKey() + " &8- &7" + join.getValue() + " joins"))
                                        .collect(Collectors.joining("\n")));

                        display.send(player);
                        i++;
                    }
                }).register("domaintracker", "joins");
    }
}

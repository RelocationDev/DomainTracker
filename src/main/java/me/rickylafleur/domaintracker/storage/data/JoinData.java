package me.rickylafleur.domaintracker.storage.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * @author Ricky Lafleur
 */

@Getter
@RequiredArgsConstructor
public final class JoinData {

    private final UUID uuid;
    private final String date, domain, country;

}
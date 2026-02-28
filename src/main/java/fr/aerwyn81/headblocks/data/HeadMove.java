package fr.aerwyn81.headblocks.data;

import org.bukkit.Location;

import java.util.UUID;

public record HeadMove(UUID hUuid, Location oldLoc) {
}

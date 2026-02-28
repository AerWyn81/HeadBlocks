package fr.aerwyn81.headblocks.holograms;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface IHologram {

    void show(Player player);

    void hide(Player player);

    void delete();

    IHologram create(String name, Location location, List<String> lines);

    boolean isVisible(Player player);

    boolean isAlive();

    void refresh(Player player);
}

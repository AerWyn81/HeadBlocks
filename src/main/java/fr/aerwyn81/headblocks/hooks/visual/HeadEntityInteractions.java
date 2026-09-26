package fr.aerwyn81.headblocks.hooks.visual;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public interface HeadEntityInteractions {

    boolean isHead(Entity entity);

    void use(Player player, Entity entity);

    void attack(Player player, Entity entity);
}

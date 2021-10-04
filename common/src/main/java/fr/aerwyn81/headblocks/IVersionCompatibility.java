package fr.aerwyn81.headblocks;

public interface IVersionCompatibility {
    Object createHeadItemStack();

    boolean isLeftHand(Object event);

    Object getItemStackInHand(Object player);

    void spawnParticle(Object location);
}

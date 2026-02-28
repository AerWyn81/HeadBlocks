package fr.aerwyn81.headblocks.data.hunt;

import fr.aerwyn81.headblocks.data.TieredReward;
import fr.aerwyn81.headblocks.services.ConfigService;

import java.util.ArrayList;
import java.util.List;

/**
 * Hunt-specific configuration overrides.
 * Null fields inherit from ConfigService (global config.yml defaults).
 */
public class HuntConfig {
    // HeadClick
    private List<String> headClickMessages;
    private Boolean headClickTitleEnabled;
    private String headClickTitleFirstLine;
    private String headClickTitleSubTitle;
    private Integer headClickTitleFadeIn;
    private Integer headClickTitleStay;
    private Integer headClickTitleFadeOut;
    private String headClickSoundFound;
    private String headClickSoundAlreadyOwn;
    private Boolean fireworkEnabled;
    private List<String> headClickCommands;
    private Boolean headClickEjectEnabled;
    private Double headClickEjectPower;

    // Holograms
    private Boolean hologramsEnabled;
    private Boolean hologramsFoundEnabled;
    private Boolean hologramsNotFoundEnabled;
    private ArrayList<String> hologramsFoundLines;
    private ArrayList<String> hologramsNotFoundLines;

    // Hints
    private Boolean hintsEnabled;
    private Integer hintDistance;
    private Integer hintFrequency;

    // Spin
    private Boolean spinEnabled;
    private Integer spinSpeed;
    private Boolean spinLinked;

    // Particles
    private Boolean particlesFoundEnabled;
    private Boolean particlesNotFoundEnabled;
    private String particlesNotFoundType;
    private Integer particlesNotFoundAmount;
    private String particlesFoundType;
    private Integer particlesFoundAmount;

    // TieredRewards
    private List<TieredReward> tieredRewards;

    // --- HeadClick getters with fallback ---

    public List<String> getHeadClickMessages() {
        return headClickMessages != null ? headClickMessages : ConfigService.getHeadClickMessages();
    }

    public void setHeadClickMessages(List<String> headClickMessages) {
        this.headClickMessages = headClickMessages;
    }

    public boolean isHeadClickTitleEnabled() {
        return headClickTitleEnabled != null ? headClickTitleEnabled : ConfigService.isHeadClickTitleEnabled();
    }

    public void setHeadClickTitleEnabled(Boolean enabled) {
        this.headClickTitleEnabled = enabled;
    }

    public String getHeadClickTitleFirstLine() {
        return headClickTitleFirstLine != null ? headClickTitleFirstLine : ConfigService.getHeadClickTitleFirstLine();
    }

    public void setHeadClickTitleFirstLine(String line) {
        this.headClickTitleFirstLine = line;
    }

    public String getHeadClickTitleSubTitle() {
        return headClickTitleSubTitle != null ? headClickTitleSubTitle : ConfigService.getHeadClickTitleSubTitle();
    }

    public void setHeadClickTitleSubTitle(String subTitle) {
        this.headClickTitleSubTitle = subTitle;
    }

    public int getHeadClickTitleFadeIn() {
        return headClickTitleFadeIn != null ? headClickTitleFadeIn : ConfigService.getHeadClickTitleFadeIn();
    }

    public void setHeadClickTitleFadeIn(Integer fadeIn) {
        this.headClickTitleFadeIn = fadeIn;
    }

    public int getHeadClickTitleStay() {
        return headClickTitleStay != null ? headClickTitleStay : ConfigService.getHeadClickTitleStay();
    }

    public void setHeadClickTitleStay(Integer stay) {
        this.headClickTitleStay = stay;
    }

    public int getHeadClickTitleFadeOut() {
        return headClickTitleFadeOut != null ? headClickTitleFadeOut : ConfigService.getHeadClickTitleFadeOut();
    }

    public void setHeadClickTitleFadeOut(Integer fadeOut) {
        this.headClickTitleFadeOut = fadeOut;
    }

    public String getHeadClickSoundFound() {
        return headClickSoundFound != null ? headClickSoundFound : ConfigService.getHeadClickNotOwnSound();
    }

    public void setHeadClickSoundFound(String sound) {
        this.headClickSoundFound = sound;
    }

    public String getHeadClickSoundAlreadyOwn() {
        return headClickSoundAlreadyOwn != null ? headClickSoundAlreadyOwn : ConfigService.getHeadClickAlreadyOwnSound();
    }

    public void setHeadClickSoundAlreadyOwn(String sound) {
        this.headClickSoundAlreadyOwn = sound;
    }

    public boolean isFireworkEnabled() {
        return fireworkEnabled != null ? fireworkEnabled : ConfigService.isFireworkEnabled();
    }

    public void setFireworkEnabled(Boolean enabled) {
        this.fireworkEnabled = enabled;
    }

    public List<String> getHeadClickCommands() {
        return headClickCommands != null ? headClickCommands : ConfigService.getHeadClickCommands();
    }

    public void setHeadClickCommands(List<String> commands) {
        this.headClickCommands = commands;
    }

    public boolean isHeadClickEjectEnabled() {
        return headClickEjectEnabled != null ? headClickEjectEnabled : ConfigService.isHeadClickEjectEnabled();
    }

    public void setHeadClickEjectEnabled(Boolean enabled) {
        this.headClickEjectEnabled = enabled;
    }

    public double getHeadClickEjectPower() {
        return headClickEjectPower != null ? headClickEjectPower : ConfigService.getHeadClickEjectPower();
    }

    public void setHeadClickEjectPower(Double power) {
        this.headClickEjectPower = power;
    }

    // --- Holograms getters with fallback ---

    public boolean isHologramsEnabled() {
        if (hologramsEnabled != null) return hologramsEnabled;
        return isHologramsFoundEnabled() || isHologramsNotFoundEnabled();
    }

    public void setHologramsEnabled(Boolean enabled) {
        this.hologramsEnabled = enabled;
    }

    public boolean isHologramsFoundEnabled() {
        return hologramsFoundEnabled != null ? hologramsFoundEnabled : ConfigService.isHologramsFoundEnabled();
    }

    public void setHologramsFoundEnabled(Boolean enabled) {
        this.hologramsFoundEnabled = enabled;
    }

    public boolean isHologramsNotFoundEnabled() {
        return hologramsNotFoundEnabled != null ? hologramsNotFoundEnabled : ConfigService.isHologramsNotFoundEnabled();
    }

    public void setHologramsNotFoundEnabled(Boolean enabled) {
        this.hologramsNotFoundEnabled = enabled;
    }

    public ArrayList<String> getHologramsFoundLines() {
        return hologramsFoundLines != null ? hologramsFoundLines : ConfigService.getHologramsFoundLines();
    }

    public void setHologramsFoundLines(ArrayList<String> lines) {
        this.hologramsFoundLines = lines;
    }

    public ArrayList<String> getHologramsNotFoundLines() {
        return hologramsNotFoundLines != null ? hologramsNotFoundLines : ConfigService.getHologramsNotFoundLines();
    }

    public void setHologramsNotFoundLines(ArrayList<String> lines) {
        this.hologramsNotFoundLines = lines;
    }

    // --- Hints getters with fallback ---

    public boolean isHintsEnabled() {
        return hintsEnabled != null ? hintsEnabled : (ConfigService.getHintDistanceBlocks() > 0);
    }

    public void setHintsEnabled(Boolean enabled) {
        this.hintsEnabled = enabled;
    }

    public int getHintDistance() {
        return hintDistance != null ? hintDistance : ConfigService.getHintDistanceBlocks();
    }

    public void setHintDistance(Integer distance) {
        this.hintDistance = distance;
    }

    public int getHintFrequency() {
        return hintFrequency != null ? hintFrequency : ConfigService.getHintFrequency();
    }

    public void setHintFrequency(Integer frequency) {
        this.hintFrequency = frequency;
    }

    // --- Spin getters with fallback ---

    public boolean isSpinEnabled() {
        return spinEnabled != null ? spinEnabled : ConfigService.isSpinEnabled();
    }

    public void setSpinEnabled(Boolean enabled) {
        this.spinEnabled = enabled;
    }

    public int getSpinSpeed() {
        return spinSpeed != null ? spinSpeed : ConfigService.getSpinSpeed();
    }

    public void setSpinSpeed(Integer speed) {
        this.spinSpeed = speed;
    }

    public boolean isSpinLinked() {
        return spinLinked != null ? spinLinked : ConfigService.isSpinLinked();
    }

    public void setSpinLinked(Boolean linked) {
        this.spinLinked = linked;
    }

    // --- Particles getters with fallback ---

    public boolean isParticlesFoundEnabled() {
        return particlesFoundEnabled != null ? particlesFoundEnabled : ConfigService.isParticlesFoundEnabled();
    }

    public void setParticlesFoundEnabled(Boolean enabled) {
        this.particlesFoundEnabled = enabled;
    }

    public boolean isParticlesNotFoundEnabled() {
        return particlesNotFoundEnabled != null ? particlesNotFoundEnabled : ConfigService.isParticlesNotFoundEnabled();
    }

    public void setParticlesNotFoundEnabled(Boolean enabled) {
        this.particlesNotFoundEnabled = enabled;
    }

    public String getParticlesNotFoundType() {
        return particlesNotFoundType != null ? particlesNotFoundType : ConfigService.getParticlesNotFoundType();
    }

    public void setParticlesNotFoundType(String type) {
        this.particlesNotFoundType = type;
    }

    public int getParticlesNotFoundAmount() {
        return particlesNotFoundAmount != null ? particlesNotFoundAmount : ConfigService.getParticlesNotFoundAmount();
    }

    public void setParticlesNotFoundAmount(Integer amount) {
        this.particlesNotFoundAmount = amount;
    }

    public String getParticlesFoundType() {
        return particlesFoundType != null ? particlesFoundType : ConfigService.getParticlesFoundType();
    }

    public void setParticlesFoundType(String type) {
        this.particlesFoundType = type;
    }

    public int getParticlesFoundAmount() {
        return particlesFoundAmount != null ? particlesFoundAmount : ConfigService.getParticlesFoundAmount();
    }

    public void setParticlesFoundAmount(Integer amount) {
        this.particlesFoundAmount = amount;
    }

    // --- TieredRewards with fallback ---

    public List<TieredReward> getTieredRewards() {
        return tieredRewards != null ? tieredRewards : ConfigService.getTieredRewards();
    }

    public void setTieredRewards(List<TieredReward> tieredRewards) {
        this.tieredRewards = tieredRewards;
    }

    // --- Raw check (is overridden?) ---

    public boolean hasHeadClickMessages() {
        return headClickMessages != null;
    }

    public boolean hasHologramsFoundLines() {
        return hologramsFoundLines != null;
    }

    public boolean hasHologramsNotFoundLines() {
        return hologramsNotFoundLines != null;
    }

    public boolean hasTieredRewards() {
        return tieredRewards != null;
    }

    public boolean hasSpinConfig() {
        return spinEnabled != null;
    }

    public boolean hasParticlesConfig() {
        return particlesFoundEnabled != null || particlesNotFoundEnabled != null;
    }

    public boolean hasHintsConfig() {
        return hintsEnabled != null;
    }
}

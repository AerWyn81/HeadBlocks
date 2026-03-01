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
    private final ConfigService configService;

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

    public HuntConfig(ConfigService configService) {
        this.configService = configService;
    }

    // --- HeadClick getters with fallback ---

    public List<String> getHeadClickMessages() {
        return headClickMessages != null ? headClickMessages : configService.headClickMessages();
    }

    public void setHeadClickMessages(List<String> headClickMessages) {
        this.headClickMessages = headClickMessages;
    }

    public boolean isHeadClickTitleEnabled() {
        return headClickTitleEnabled != null ? headClickTitleEnabled : configService.headClickTitleEnabled();
    }

    public void setHeadClickTitleEnabled(Boolean enabled) {
        this.headClickTitleEnabled = enabled;
    }

    public String getHeadClickTitleFirstLine() {
        return headClickTitleFirstLine != null ? headClickTitleFirstLine : configService.headClickTitleFirstLine();
    }

    public void setHeadClickTitleFirstLine(String line) {
        this.headClickTitleFirstLine = line;
    }

    public String getHeadClickTitleSubTitle() {
        return headClickTitleSubTitle != null ? headClickTitleSubTitle : configService.headClickTitleSubTitle();
    }

    public void setHeadClickTitleSubTitle(String subTitle) {
        this.headClickTitleSubTitle = subTitle;
    }

    public int getHeadClickTitleFadeIn() {
        return headClickTitleFadeIn != null ? headClickTitleFadeIn : configService.headClickTitleFadeIn();
    }

    public void setHeadClickTitleFadeIn(Integer fadeIn) {
        this.headClickTitleFadeIn = fadeIn;
    }

    public int getHeadClickTitleStay() {
        return headClickTitleStay != null ? headClickTitleStay : configService.headClickTitleStay();
    }

    public void setHeadClickTitleStay(Integer stay) {
        this.headClickTitleStay = stay;
    }

    public int getHeadClickTitleFadeOut() {
        return headClickTitleFadeOut != null ? headClickTitleFadeOut : configService.headClickTitleFadeOut();
    }

    public void setHeadClickTitleFadeOut(Integer fadeOut) {
        this.headClickTitleFadeOut = fadeOut;
    }

    public String getHeadClickSoundFound() {
        return headClickSoundFound != null ? headClickSoundFound : configService.headClickNotOwnSound();
    }

    public void setHeadClickSoundFound(String sound) {
        this.headClickSoundFound = sound;
    }

    public String getHeadClickSoundAlreadyOwn() {
        return headClickSoundAlreadyOwn != null ? headClickSoundAlreadyOwn : configService.headClickAlreadyOwnSound();
    }

    public void setHeadClickSoundAlreadyOwn(String sound) {
        this.headClickSoundAlreadyOwn = sound;
    }

    public boolean isFireworkEnabled() {
        return fireworkEnabled != null ? fireworkEnabled : configService.fireworkEnabled();
    }

    public void setFireworkEnabled(Boolean enabled) {
        this.fireworkEnabled = enabled;
    }

    public List<String> getHeadClickCommands() {
        return headClickCommands != null ? headClickCommands : configService.headClickCommands();
    }

    public void setHeadClickCommands(List<String> commands) {
        this.headClickCommands = commands;
    }

    public boolean isHeadClickEjectEnabled() {
        return headClickEjectEnabled != null ? headClickEjectEnabled : configService.headClickEjectEnabled();
    }

    public void setHeadClickEjectEnabled(Boolean enabled) {
        this.headClickEjectEnabled = enabled;
    }

    public double getHeadClickEjectPower() {
        return headClickEjectPower != null ? headClickEjectPower : configService.headClickEjectPower();
    }

    public void setHeadClickEjectPower(Double power) {
        this.headClickEjectPower = power;
    }

    public boolean isHologramsEnabled() {
        if (hologramsEnabled != null) {
            return hologramsEnabled;
        }
        return isHologramsFoundEnabled() || isHologramsNotFoundEnabled();
    }

    public void setHologramsEnabled(Boolean enabled) {
        this.hologramsEnabled = enabled;
    }

    public boolean isHologramsFoundEnabled() {
        return hologramsFoundEnabled != null ? hologramsFoundEnabled : configService.hologramsFoundEnabled();
    }

    public void setHologramsFoundEnabled(Boolean enabled) {
        this.hologramsFoundEnabled = enabled;
    }

    public boolean isHologramsNotFoundEnabled() {
        return hologramsNotFoundEnabled != null ? hologramsNotFoundEnabled : configService.hologramsNotFoundEnabled();
    }

    public void setHologramsNotFoundEnabled(Boolean enabled) {
        this.hologramsNotFoundEnabled = enabled;
    }

    public ArrayList<String> getHologramsFoundLines() {
        return hologramsFoundLines != null ? hologramsFoundLines : configService.hologramsFoundLines();
    }

    public void setHologramsFoundLines(ArrayList<String> lines) {
        this.hologramsFoundLines = lines;
    }

    public ArrayList<String> getHologramsNotFoundLines() {
        return hologramsNotFoundLines != null ? hologramsNotFoundLines : configService.hologramsNotFoundLines();
    }

    public void setHologramsNotFoundLines(ArrayList<String> lines) {
        this.hologramsNotFoundLines = lines;
    }

    // --- Hints getters with fallback ---

    public boolean isHintsEnabled() {
        return hintsEnabled != null ? hintsEnabled : (configService.hintDistanceBlocks() > 0);
    }

    public void setHintsEnabled(Boolean enabled) {
        this.hintsEnabled = enabled;
    }

    public int getHintDistance() {
        return hintDistance != null ? hintDistance : configService.hintDistanceBlocks();
    }

    public void setHintDistance(Integer distance) {
        this.hintDistance = distance;
    }

    public int getHintFrequency() {
        return hintFrequency != null ? hintFrequency : configService.hintFrequency();
    }

    public void setHintFrequency(Integer frequency) {
        this.hintFrequency = frequency;
    }

    // --- Spin getters with fallback ---

    public boolean isSpinEnabled() {
        return spinEnabled != null ? spinEnabled : configService.spinEnabled();
    }

    public void setSpinEnabled(Boolean enabled) {
        this.spinEnabled = enabled;
    }

    public int getSpinSpeed() {
        return spinSpeed != null ? spinSpeed : configService.spinSpeed();
    }

    public void setSpinSpeed(Integer speed) {
        this.spinSpeed = speed;
    }

    public boolean isSpinLinked() {
        return spinLinked != null ? spinLinked : configService.spinLinked();
    }

    public void setSpinLinked(Boolean linked) {
        this.spinLinked = linked;
    }

    // --- Particles getters with fallback ---

    public boolean isParticlesFoundEnabled() {
        return particlesFoundEnabled != null ? particlesFoundEnabled : configService.particlesFoundEnabled();
    }

    public void setParticlesFoundEnabled(Boolean enabled) {
        this.particlesFoundEnabled = enabled;
    }

    public boolean isParticlesNotFoundEnabled() {
        return particlesNotFoundEnabled != null ? particlesNotFoundEnabled : configService.particlesNotFoundEnabled();
    }

    public void setParticlesNotFoundEnabled(Boolean enabled) {
        this.particlesNotFoundEnabled = enabled;
    }

    public String getParticlesNotFoundType() {
        return particlesNotFoundType != null ? particlesNotFoundType : configService.particlesNotFoundType();
    }

    public void setParticlesNotFoundType(String type) {
        this.particlesNotFoundType = type;
    }

    public int getParticlesNotFoundAmount() {
        return particlesNotFoundAmount != null ? particlesNotFoundAmount : configService.particlesNotFoundAmount();
    }

    public void setParticlesNotFoundAmount(Integer amount) {
        this.particlesNotFoundAmount = amount;
    }

    public String getParticlesFoundType() {
        return particlesFoundType != null ? particlesFoundType : configService.particlesFoundType();
    }

    public void setParticlesFoundType(String type) {
        this.particlesFoundType = type;
    }

    public int getParticlesFoundAmount() {
        return particlesFoundAmount != null ? particlesFoundAmount : configService.particlesFoundAmount();
    }

    public void setParticlesFoundAmount(Integer amount) {
        this.particlesFoundAmount = amount;
    }

    // --- TieredRewards with fallback ---

    public List<TieredReward> getTieredRewards() {
        return tieredRewards != null ? tieredRewards : configService.tieredRewards();
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

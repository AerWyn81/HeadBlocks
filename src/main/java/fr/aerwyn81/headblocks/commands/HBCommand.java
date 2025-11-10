package fr.aerwyn81.headblocks.commands;

public class HBCommand {
    private final Cmd cmdClass;
    private final String command;
    private final String permission;
    private final boolean isPlayerCommand;
    private final String[] args;
    private final boolean visible;
    private final String alias;

    public HBCommand(Cmd command) {
        this.cmdClass = command;
        this.command = cmdClass.getClass().getAnnotation(HBAnnotations.class).command();
        this.permission = cmdClass.getClass().getAnnotation(HBAnnotations.class).permission();
        this.isPlayerCommand = cmdClass.getClass().getAnnotation(HBAnnotations.class).isPlayerCommand();
        this.args = cmdClass.getClass().getAnnotation(HBAnnotations.class).args();
        this.visible = cmdClass.getClass().getAnnotation(HBAnnotations.class).isVisible();
        this.alias = cmdClass.getClass().getAnnotation(HBAnnotations.class).alias();
    }

    public Cmd getCmdClass() {
        return cmdClass;
    }

    public String getCommand() {
        return command;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isPlayerCommand() {
        return isPlayerCommand;
    }

    public String[] getArgs() {
        return args;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getAlias() {
        return alias;
    }
}

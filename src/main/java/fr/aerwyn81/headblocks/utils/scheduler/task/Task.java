package fr.aerwyn81.headblocks.utils.scheduler.task;

public interface Task {

    boolean isCancelled();

    void cancel();

}

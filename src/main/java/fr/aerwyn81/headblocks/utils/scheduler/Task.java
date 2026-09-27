package fr.aerwyn81.headblocks.utils.scheduler;

public interface Task {

    Task NONE = new Task() {
        @Override
        public void cancel() {
            // nothing to cancel: this task was never scheduled
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    void cancel();

    boolean isCancelled();
}

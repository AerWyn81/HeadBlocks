package fr.aerwyn81.headblocks.utils.scheduler;

public interface Task {

    Task NONE = new Task() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    void cancel();

    boolean isCancelled();
}

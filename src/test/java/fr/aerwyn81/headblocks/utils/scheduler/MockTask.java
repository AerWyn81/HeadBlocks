package fr.aerwyn81.headblocks.utils.scheduler;

import fr.aerwyn81.headblocks.utils.scheduler.task.Task;

public class MockTask implements Task {
    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void cancel() {

    }
}

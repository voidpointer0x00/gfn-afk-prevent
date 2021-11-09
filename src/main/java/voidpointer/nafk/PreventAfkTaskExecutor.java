/*
 * Copyright (C) 2021 Vasiliy Petukhov <void.pointer@ya.ru>
 */

package voidpointer.nafk;

import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PreventAfkTaskExecutor {
    private static final long INITIAL_DELAY = 5;
    private static final long DELAY_IN_SECONDS = 120;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final PreventAfkTask preventAfkTask;

    public PreventAfkTaskExecutor() throws AWTException {
        preventAfkTask = new PreventAfkTask(new Robot());
    }

    public void start() {
        executor.scheduleWithFixedDelay(preventAfkTask, INITIAL_DELAY, DELAY_IN_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        preventAfkTask.stop();
        executor.shutdown();
    }
}

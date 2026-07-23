package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.TaskManager;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class BukkitTaskManager extends TaskManager {

    private final BukkitSchedulerAdapter schedulerAdapter;

    public BukkitTaskManager(final Plugin plugin) {
        this.schedulerAdapter = new BukkitSchedulerAdapter(plugin);
    }

    public static boolean isTaskThread() {
        return BukkitSchedulerAdapter.isGlobalTaskThread();
    }

    @Override
    public int repeat(@Nonnull final Runnable runnable, final int interval) {
        return schedulerAdapter.repeat(runnable, interval);
    }

    @Override
    public int repeatAsync(@Nonnull final Runnable runnable, final int interval) {
        return schedulerAdapter.repeatAsync(runnable, interval);
    }

    @Override
    public void async(@Nonnull final Runnable runnable) {
        schedulerAdapter.async(runnable);
    }

    @Override
    public void task(@Nonnull final Runnable runnable) {
        schedulerAdapter.task(runnable);
    }

    @Override
    public void later(@Nonnull final Runnable runnable, final int delay) {
        schedulerAdapter.later(runnable, delay);
    }

    @Override
    public void laterAsync(@Nonnull final Runnable runnable, final int delay) {
        schedulerAdapter.laterAsync(runnable, delay);
    }

    @Override
    public void cancel(final int task) {
        schedulerAdapter.cancel(task);
    }

}

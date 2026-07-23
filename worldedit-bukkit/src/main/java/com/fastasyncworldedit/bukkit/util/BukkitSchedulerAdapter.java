package com.fastasyncworldedit.bukkit.util;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class BukkitSchedulerAdapter {

    private static final ThreadLocal<Boolean> GLOBAL_TASK_THREAD = ThreadLocal.withInitial(() -> false);

    private final Plugin plugin;
    private final AtomicInteger nextTaskId = new AtomicInteger(1);
    private final Map<Integer, Runnable> taskCancellers = new ConcurrentHashMap<>();

    public BukkitSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isGlobalTaskThread() {
        return GLOBAL_TASK_THREAD.get();
    }

    public static void cancelTasks(Plugin plugin) {
        if (PaperLib.isPaper()) {
            Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            Bukkit.getAsyncScheduler().cancelTasks(plugin);
            return;
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    public int repeat(Runnable runnable, int interval) {
        return repeat(runnable, interval, interval);
    }

    public int repeat(Runnable runnable, long delay, long period) {
        if (PaperLib.isPaper()) {
            var scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    plugin,
                    task -> runGlobalTask(runnable),
                    normalizeTicks(delay),
                    normalizeTicks(period)
            );
            return register(scheduledTask::cancel);
        }
        return plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> runGlobalTask(runnable), delay, period);
    }

    public int repeatAsync(Runnable runnable, int interval) {
        if (PaperLib.isPaper()) {
            long intervalMillis = ticksToMillis(normalizeTicks(interval));
            var scheduledTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin,
                    task -> runnable.run(),
                    intervalMillis,
                    intervalMillis,
                    TimeUnit.MILLISECONDS
            );
            return register(scheduledTask::cancel);
        }
        return plugin.getServer().getScheduler().scheduleAsyncRepeatingTask(plugin, runnable, interval, interval);
    }

    public void async(Runnable runnable) {
        if (PaperLib.isPaper()) {
            Bukkit.getAsyncScheduler().runNow(plugin, task -> runnable.run());
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public void task(Runnable runnable) {
        if (PaperLib.isPaper()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, task -> runGlobalTask(runnable));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> runGlobalTask(runnable));
    }

    public void later(Runnable runnable, int delay) {
        if (delay <= 0) {
            task(runnable);
            return;
        }
        if (PaperLib.isPaper()) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> runGlobalTask(runnable), delay);
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> runGlobalTask(runnable), delay);
    }

    public void laterAsync(Runnable runnable, int delay) {
        if (PaperLib.isPaper()) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> runnable.run(), ticksToMillis(delay), TimeUnit.MILLISECONDS);
            return;
        }
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, runnable, delay);
    }

    public void cancel(int taskId) {
        Runnable taskCanceller = taskCancellers.remove(taskId);
        if (taskCanceller != null) {
            taskCanceller.run();
            return;
        }
        if (!PaperLib.isPaper() && taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private int register(Runnable taskCanceller) {
        int scheduledTaskId = nextTaskId.getAndIncrement();
        taskCancellers.put(scheduledTaskId, taskCanceller);
        return scheduledTaskId;
    }

    private void runGlobalTask(Runnable runnable) {
        GLOBAL_TASK_THREAD.set(true);
        try {
            runnable.run();
        } finally {
            GLOBAL_TASK_THREAD.remove();
        }
    }

    private long normalizeTicks(long ticks) {
        return Math.max(1L, ticks);
    }

    private long ticksToMillis(long ticks) {
        return Math.max(0L, ticks) * 50L;
    }

}

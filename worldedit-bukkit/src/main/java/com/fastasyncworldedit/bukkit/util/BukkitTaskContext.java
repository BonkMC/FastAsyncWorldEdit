package com.fastasyncworldedit.bukkit.util;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class BukkitTaskContext {

    private BukkitTaskContext() {
    }

    public static <T> T callEntity(Plugin plugin, Entity entity, Supplier<T> taskAction, Supplier<T> retiredAction) {
        if (canAccess(entity)) {
            return taskAction.get();
        }
        if (PaperLib.isPaper()) {
            CompletableFuture<T> scheduledFuture = new CompletableFuture<>();
            var scheduledTask = entity.getScheduler().run(
                    plugin,
                    task -> complete(scheduledFuture, taskAction),
                    () -> complete(scheduledFuture, retiredAction)
            );
            if (scheduledTask == null) {
                complete(scheduledFuture, retiredAction);
            }
            return scheduledFuture.join();
        }
        return callSync(plugin, taskAction);
    }

    public static <T> T callRegion(Plugin plugin, Location location, Supplier<T> taskAction) {
        if (canAccess(location)) {
            return taskAction.get();
        }
        if (PaperLib.isPaper()) {
            CompletableFuture<T> scheduledFuture = new CompletableFuture<>();
            Bukkit.getRegionScheduler().run(plugin, location, task -> complete(scheduledFuture, taskAction));
            return scheduledFuture.join();
        }
        return callSync(plugin, taskAction);
    }

    public static boolean canAccess(Entity entity) {
        if (PaperLib.isPaper()) {
            return Bukkit.isOwnedByCurrentRegion(entity);
        }
        return Bukkit.isPrimaryThread();
    }

    public static boolean canAccess(Location location) {
        if (PaperLib.isPaper()) {
            return Bukkit.isOwnedByCurrentRegion(location);
        }
        return Bukkit.isPrimaryThread();
    }

    private static <T> T callSync(Plugin plugin, Supplier<T> taskAction) {
        if (Bukkit.isPrimaryThread()) {
            return taskAction.get();
        }
        try {
            return Bukkit.getScheduler().callSyncMethod(plugin, taskAction::get).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Bukkit task execution.", exception);
        } catch (ExecutionException exception) {
            throw new RuntimeException("Bukkit task execution failed.", exception);
        }
    }

    private static <T> void complete(CompletableFuture<T> scheduledFuture, Supplier<T> taskAction) {
        try {
            scheduledFuture.complete(taskAction.get());
        } catch (Throwable throwable) {
            scheduledFuture.completeExceptionally(throwable);
        }
    }

}

package com.kamuiprison.prison;

import com.kamuiprison.KamuiPrisonMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Agendador simples de tarefas por tick (somente thread do servidor). */
public final class ServerTasks {
    private static final class Task {
        int ticks;
        final Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private ServerTasks() {
    }

    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(Math.max(1, delayTicks), action));
    }

    public static void tick() {
        if (TASKS.isEmpty()) {
            return;
        }
        List<Runnable> due = new ArrayList<>();
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            task.ticks--;
            if (task.ticks <= 0) {
                due.add(task.action);
                it.remove();
            }
        }
        for (Runnable action : due) {
            try {
                action.run();
            } catch (Throwable t) {
                KamuiPrisonMod.LOGGER.error("Erro em tarefa agendada do Kamui: Prisao", t);
            }
        }
    }

    public static void clear() {
        TASKS.clear();
    }
}

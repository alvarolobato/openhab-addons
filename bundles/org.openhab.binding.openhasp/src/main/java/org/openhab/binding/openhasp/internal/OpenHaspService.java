package org.openhab.binding.openhasp.internal;

import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.openhab.core.common.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenHaspService {

    private final Logger logger = LoggerFactory.getLogger(OpenHaspService.class);

    private static OpenHaspService instance = null;
    private static final String THREADPOOL_NAME = "OpenHaspService";

    private final Set<OpenHASPPlateListener> listeners = new CopyOnWriteArraySet<>();

    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(THREADPOOL_NAME);

    private ScheduledFuture<?> future = null;

    private final Runnable timerRunnable = () -> {
        synchronized (listeners) {
            notifyOnTimerEvent();
        }
    };

    private OpenHaspService() {
    }

    /**
     * Returns the {@link OpenHaspService} singleton instance.
     *
     * @return The {@link OpenHaspService} singleton
     */
    public static synchronized OpenHaspService instance() {
        if (instance == null) {
            instance = new OpenHaspService();
        }
        return instance;
    }

    /**
     * Registers the given {@link PcapNetworkInterfaceListener}. If it is already
     * registered, this method returns
     * immediately.
     *
     * @param networkInterfaceListener The {@link PcapNetworkInterfaceListener} to
     *            be registered.
     */
    public void registerListener(OpenHASPPlateListener networkInterfaceListener) {
        final boolean isAdded = listeners.add(networkInterfaceListener);
        if (isAdded) {
            updatePollingState();
        }
    }

    /**
     * Unregisters the given {@link PcapNetworkInterfaceListener}. If it is already
     * unregistered, this method returns
     * immediately.
     *
     * @param networkInterfaceListener The {@link PcapNetworkInterfaceListener} to
     *            be unregistered.
     */
    public void unregisterListener(OpenHASPPlateListener networkInterfaceListener) {
        final boolean isRemoved = listeners.remove(networkInterfaceListener);
        if (isRemoved) {
            updatePollingState();
        }
    }

    private void notifyOnTimerEvent() {
        for (OpenHASPPlateListener listener : listeners) {
            try {
                listener.onTimerEvent();
            } catch (Exception e) {
                logger.error("An exception occurred while calling onTimerEvent for {}", listener, e);
            }
        }
    }

    private void updatePollingState() {
        boolean isPolling = future != null;
        if (isPolling && listeners.isEmpty()) {
            future.cancel(true);
            future = null;
            return;
        }
        if (!isPolling && !listeners.isEmpty()) {
            // Get the current time to calculate the initial delay until the next minute starts
            Calendar now = Calendar.getInstance();
            int secondsUntilNextMinute = 60 - now.get(Calendar.SECOND);
            scheduler.schedule(timerRunnable, 1, TimeUnit.SECONDS);
            // future = scheduler.scheduleWithFixedDelay(timerRunnable, secondsUntilNextMinute, 60, TimeUnit.SECONDS);
            future = scheduler.scheduleWithFixedDelay(timerRunnable, secondsUntilNextMinute, 60, TimeUnit.SECONDS);
        }
    }
}

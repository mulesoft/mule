package org.mule.runtime.core.api.lifecycle;

import static java.lang.System.nanoTime;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisposedObjectsTracker {

    private static final long POSSIBLE_LEAK_TIMEOUT = 15000000000L;

    private static final Logger LOGGER = LoggerFactory.getLogger(DisposedObjectsTracker.class);

    private Set<LeakTrackReference> disposedObjects;

    public DisposedObjectsTracker() {
        disposedObjects = new HashSet<>();

        Thread maintainerThread = new Thread(this::maintain);
        maintainerThread.start();
        maintainerThread.setDaemon(true);
    }

    public synchronized void markThatShouldBeCollected(Object object) {
        if (object == null) {
            return;
        }
        disposedObjects.add(new LeakTrackReference(object));
    }

    synchronized void maintain() {
        Set<LeakTrackReference> notYetCollected = new HashSet<>(disposedObjects.size());
        for (LeakTrackReference ref : disposedObjects) {
            if (!ref.isCollected()) {
                notYetCollected.add(ref);
            }
            if (ref.isPossibleLeak()) {
                LOGGER.warn("Possible leak of object {}. It was disposed, but it's still in memory after {} nanoseconds", ref.get(), POSSIBLE_LEAK_TIMEOUT);
            }
        }
    }

    private static class LeakTrackReference<T> {
        private final WeakReference<T> weakReference;
        private final PhantomReference<T> phantomReference;
        private final long disposeTimestamp;

        public LeakTrackReference(T object) {
            disposeTimestamp = nanoTime();
            weakReference = new WeakReference<>(object);
            phantomReference = new PhantomReference<>(object, new ReferenceQueue<>());
        }

        boolean isCollected() {
            return phantomReference.isEnqueued();
        }

        boolean isPossibleLeak() {
            return !isCollected() && timeoutElapsed();
        }

        private boolean timeoutElapsed() {
            return nanoTime() > disposeTimestamp + POSSIBLE_LEAK_TIMEOUT;
        }

        public Object get() {
            return weakReference.get();
        }
    }
}

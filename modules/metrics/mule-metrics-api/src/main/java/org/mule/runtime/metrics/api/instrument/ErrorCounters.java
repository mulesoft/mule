package org.mule.runtime.metrics.api.instrument;

import java.util.function.Consumer;

public interface ErrorCounters extends Instrument {

    void add(Error value);

    void add(Throwable value);

    void onNewError(Consumer<LongCounter> newErrorCounterConsumer);
}

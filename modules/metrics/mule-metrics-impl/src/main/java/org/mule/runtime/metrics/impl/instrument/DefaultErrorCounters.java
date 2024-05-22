package org.mule.runtime.metrics.impl.instrument;

import org.mule.runtime.core.api.util.UUID;
import org.mule.runtime.metrics.api.instrument.ErrorCounters;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

public class DefaultErrorCounters implements ErrorCounters {

    private final Meter meter;
    private final String name;
    private final String description;
    private final Map<String, LongCounter> errorCounters = new ConcurrentHashMap<>();
    private final Set<Consumer<LongCounter>> onNewErrorConsumers = new HashSet<>();

    // TODO: Include total error count

    private DefaultErrorCounters(String name, String description, Meter meter) {
        this.name = name;
        this.description = description;
        this.meter = meter;
    }

    public static DefaultErrorsCountersBuilder builder(String name, String description, Meter meter) {
        return new DefaultErrorsCountersBuilder(name, description, meter);
    }

    @Override
    public void add(Error error) {
        errorCounters.computeIfAbsent(getErrorId(error), this::buildNewErrorCounter).add(1);
    }

    @Override
    public void add(Throwable error) {
        String errorId = getErrorId(error);
        errorCounters.getOrDefault(errorId, buildNewErrorCounter(errorId));
    }

    @Override
    public void onNewError(Consumer<LongCounter> newErrorCounterConsumer) {
        onNewErrorConsumers.add(newErrorCounterConsumer);
    }

    private LongCounter buildNewErrorCounter(String errorId) {
        LongCounter newErrorCounter = DefaultLongCounter.builder("error@" + errorId + "-count", getMeter()).withDescription("Mule runtime error count").build();
        onNewError(newErrorCounter);
        return newErrorCounter;
    }

    private void onNewError(LongCounter newErrorCounter) {
        onNewErrorConsumers.forEach(longCounterConsumer -> longCounterConsumer.accept(newErrorCounter));
    }

    private String getErrorId(Error error) {
        // TODO Implement error id logic.
        return UUID.getUUID();
    }

    private String getErrorId(Throwable error) {
        // TODO Implement error id logic.
        return UUID.getUUID();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Meter getMeter() {
        return meter;
    }

    public static class DefaultErrorsCountersBuilder {
        private final String name;
        private final String description;
        private final Meter meter;
        private InstrumentRepository instrumentRepository;
        private MeterExporter meterExporter;

        private DefaultErrorsCountersBuilder(String name, String description, Meter meter) {
            this.name = name;
            this.description = description;
            this.meter = meter;
        }

        public DefaultErrorsCountersBuilder withInstrumentRepository(InstrumentRepository instrumentRepository) {
            this.instrumentRepository = instrumentRepository;
            return this;
        }

        public DefaultErrorsCountersBuilder withMeterExporter(MeterExporter meterExporter) {
            this.meterExporter = meterExporter;
            return this;
        }

        public ErrorCounters build() {
            ErrorCounters errorCounters = ofNullable(instrumentRepository)
                    .map(repository -> (ErrorCounters) repository.create(name, name -> doBuild()))
                    .orElse(doBuild());
            if (meterExporter != null) {
                meterExporter.enableExport(errorCounters);
            }
            return errorCounters;
        }

        private ErrorCounters doBuild() {
            return new DefaultErrorCounters(name, description, meter);
        }
    }
}

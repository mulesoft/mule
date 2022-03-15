package org.mule.runtime.core.internal.profiling.producer;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.ResettableProfilingDataProducer;
import org.mule.runtime.core.internal.profiling.context.DefaultComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;

import java.util.function.Function;

public class TransactionProfilingDataProducer implements
        ResettableProfilingDataProducer<TransactionProfilingEventContext, CoreEvent> {
    @Override
    public void reset() {

    }

    @Override
    public void triggerProfilingEvent(TransactionProfilingEventContext transactionProfilingEventContext) {

    }

    @Override
    public void triggerProfilingEvent(CoreEvent coreEvent, Function<CoreEvent, TransactionProfilingEventContext> function) {

    }
}

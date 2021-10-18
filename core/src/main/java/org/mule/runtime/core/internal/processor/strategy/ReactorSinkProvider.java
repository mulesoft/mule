package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.FluxSink;

public interface ReactorSinkProvider {

    FluxSink<CoreEvent> getSink();

    void accept(ReactorSinkProviderBasedSink reactorSinkProviderBasedSink, CoreEvent event);

    void dispose();
}

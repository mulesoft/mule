/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.FluxSink;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ReactorSinkProviderBasedSinkTestCase {

    @Test
    public void sinkCompletedAfterThreadTermination() throws InterruptedException {

        FluxSink<CoreEvent> fluxSink = (FluxSink<CoreEvent>) mock(FluxSink.class);
        ReactorSinkProvider sinkProvider = new AbstractReactorSinkProvider() {
            @Override
            public FluxSink<CoreEvent> getSink() {
                return fluxSink;
            }
        };
        ReactorSinkProviderBasedSink reactorSinkProviderBasedSink = new ReactorSinkProviderBasedSink(sinkProvider);

        Thread thread = new Thread(() -> {
            reactorSinkProviderBasedSink.accept(mock(CoreEvent.class));
        });

        thread.start();

        thread.join();

        verify(fluxSink).complete();
    }
}

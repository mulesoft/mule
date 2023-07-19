/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.FluxSink;

/**
 * Provider of {@link FluxSink<CoreEvent>}.
 */
public interface ReactorSinkProvider extends Disposable {

  FluxSink<CoreEvent> getSink();
}

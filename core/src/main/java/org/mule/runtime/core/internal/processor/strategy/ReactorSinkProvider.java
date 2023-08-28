/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

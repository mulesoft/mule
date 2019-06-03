/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import org.mule.runtime.api.lifecycle.Disposable;
import reactor.core.publisher.FluxSink;

import java.util.function.Supplier;

/**
 * Supplier of {@link FluxSink}.
 *
 * @param <T> the value type
 *
 * @since 4.3
 */
public interface FluxSinkSupplier<T> extends Supplier<FluxSink<T>>, Disposable {
}

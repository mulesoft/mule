/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;

import java.util.function.Supplier;

public interface RecordSpanErrorMethod<T> {

  void recordError(T context, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan, FlowCallStack flowCallStack);
}

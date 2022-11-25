/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.Optional;

/**
 * @param <T> the carrier for the {@link SpanContext}
 *
 * @since 4.5.0
 */
public interface StartEventSpanMethod<T> {

  Optional<InternalSpan> start(T context, CoreEvent coreEvent,
                               StartSpanInfo spanCustomizationInfo);

  Optional<InternalSpan> start(T context, CoreEvent coreEvent, StartSpanInfo spanCustomizationInfo,
                               Assertion assertion);
}

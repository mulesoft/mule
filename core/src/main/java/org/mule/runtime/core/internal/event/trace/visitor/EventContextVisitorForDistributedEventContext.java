/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.event.trace.visitor;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;

/**
 * An {@link EventContextVisitor} that visits an {@link org.mule.runtime.api.event.EventContext} and returns a
 * {@link DistributedTraceContext}.
 *
 * @since 4.5.0
 */
public interface EventContextVisitorForDistributedEventContext extends EventContextVisitor<DistributedTraceContext> {
}

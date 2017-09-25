/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Iterator;
import java.util.List;

/**
 * Implementations must provide a way to wrap each element in a {@link CoreEvent}.
 *
 * @since 4.0
 */
public interface EventBuilderConfigurerList<T> extends List<T> {

  Iterator<EventBuilderConfigurer> eventBuilderConfigurerIterator();

}

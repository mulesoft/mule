/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

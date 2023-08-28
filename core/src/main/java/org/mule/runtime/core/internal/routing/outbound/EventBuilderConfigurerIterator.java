/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Iterator;

/**
 * Implementations must provide a way to wrap each element in a {@link CoreEvent}.
 *
 * @since 4.0
 */
public interface EventBuilderConfigurerIterator<T> extends Iterator<T> {

  EventBuilderConfigurer nextEventBuilderConfigurer();

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import org.mule.api.annotation.Experimental;

import java.util.Set;

/**
 * An strategy for discovering the available instances of {@link ProfilingDataConsumer}.
 */
@Experimental
public interface ProfilerDataConsumerDiscoveryStrategy {

  <S extends ProfilingDataConsumer<T>, T extends ProfilingEventContext> Set<S> discover();

}

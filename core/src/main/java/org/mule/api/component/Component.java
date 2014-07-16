/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.component;

import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.management.stats.ComponentStatistics;

/**
 * A <code>Component</code> component processes a {@link MuleEvent} by invoking the
 * component instance that has been configured, optionally returning a result.
 * <p/>
 * Implementations of <code>Component</code> can use different types of component
 * implementation, implement component instance pooling or implement
 * <em>bindings</em> which allow for service composition.
 */
public interface Component extends MessageProcessor, FlowConstructAware
{

    /**
     * Component statistics are used to gather component statistics such as
     * sync/async invocation counts and total and average execution time.
     */
    ComponentStatistics getStatistics();

}

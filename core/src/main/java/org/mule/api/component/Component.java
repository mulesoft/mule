/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

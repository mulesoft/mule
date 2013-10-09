/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.construct;

import org.mule.api.MuleContext;
import org.mule.api.NamedObject;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.source.MessageSource;
import org.mule.management.stats.FlowConstructStatistics;

/**
 * A uniquely identified {@link FlowConstruct} that once implemented and configured defines a construct
 * through which messages are processed using {@link MessageSource} and {@link MessageProcessor} building
 * blocks.
 */
public interface FlowConstruct extends NamedObject, LifecycleStateEnabled
{

    /**
     * @return The exception listener that will be used to handle exceptions that may be thrown at different
     *         points during the message flow defined by this construct.
     */
    MessagingExceptionHandler getExceptionListener();

    /**
     * @return The statistics holder used by this flow construct to keep track of its activity.
     */
    FlowConstructStatistics getStatistics();

    /**
     * @return This implementation of {@link MessageInfoMapping} used to control how Important message
     *         information is pulled from the current message.
     */
    MessageInfoMapping getMessageInfoMapping();

    /**
     * @return This muleContext that this flow construct belongs to and runs in the context of.
     */
    MuleContext getMuleContext();

}

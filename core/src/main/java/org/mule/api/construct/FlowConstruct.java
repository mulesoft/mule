/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.construct;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleStateEnabled;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.management.stats.FlowConstructStatistics;

import java.beans.ExceptionListener;

/**
 * <p>
 * A uniquely identified message flow construct who's implementation and
 * configuration defines at a minimum where messages come from and what processing
 * steps should be used to process these messages.
 * <p/>
 */
public interface FlowConstruct extends LifecycleStateEnabled
{

    /**
     * @return The name which identifies this flow construct which is unique in
     *         MuleConext registry
     */
    String getName();

    /**
     * @return The exception listener that will be used to handle exceptions that may
     *         be thrown at different points during the message flow defined by this
     *         construct.
     */
    ExceptionListener getExceptionListener();

    /**
     * @return The statistics holder used by this flow construct to keep track of its
     *         activity.
     */
    FlowConstructStatistics getStatistics();

    MuleContext getMuleContext();

    /**
     * @return This implementation of {@link MessageInfoMapping} used to control how
     *         Important message information is pulled from the current message.
     */
    MessageInfoMapping getMessageInfoMapping();
}

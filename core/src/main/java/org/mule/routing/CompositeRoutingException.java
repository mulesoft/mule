/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageRouter;
import org.mule.config.i18n.Message;

import java.util.Collections;
import java.util.Map;

/**
 * This is a {@link MessagingException} used to aggregate exceptions thrown by
 * several routes in the context of a single {@link MessageRouter} Exceptions are
 * correlated to each route through a sequential id
 * 
 * @since 3.5.0
 */
public class CompositeRoutingException extends MessagingException
{

    private static final long serialVersionUID = -4421728527040579607L;

    private final Map<Integer, Exception> exceptions;

    /**
     * Constructs a new {@link CompositeRoutingException}
     * 
     * @param message message describing the failure
     * @param event the current {@link MuleEvent}
     * @param exceptions a {@link Map} in which the key is an {@link Integer}
     *            describing the index of the route that generated the error and the
     *            value is the {@link Exception} itself
     */
    public CompositeRoutingException(Message message, MuleEvent event, Map<Integer, Exception> exceptions)
    {
        super(message, event);
        this.exceptions = Collections.unmodifiableMap(exceptions);
    }

    /**
     * Returns the {@link Exception} for the given route index
     * 
     * @param index the index of a failing route
     * @return an {@link Exception} or <code>null</code> if no {@link Exception} was
     *         found for that index
     */
    public Exception getExceptionForRouteIndex(Integer index)
    {
        return this.exceptions.get(index);
    }

    /**
     * @return a {@link Map} in which the key is an {@link Integer} describing the
     *         number of the route that generated the error and the value is the
     *         {@link Exception} itself
     */
    public Map<Integer, Exception> getExceptions()
    {
        return this.exceptions;
    }
}

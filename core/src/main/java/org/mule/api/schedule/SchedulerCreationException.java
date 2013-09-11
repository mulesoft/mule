/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.schedule;

/**
 * <p>
 * This exception is thrown if a {@link Scheduler} could not be created.
 * </p>
 *
 * @since 3.5.0
 */
public class SchedulerCreationException extends RuntimeException
{

    public SchedulerCreationException(String s, Throwable throwable)
    {
        super(s, throwable);
    }

    public SchedulerCreationException(String s)
    {
        super(s);
    }
}

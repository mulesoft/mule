/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.time;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.core.api.MuleContext;

import java.util.function.Supplier;

/**
 * A {@link Supplier} which provides the current system time
 * in milliseconds.
 *
 * @since 4.0
 */
public class TimeSupplier implements Supplier<Long>
{

    /**
     * Returns the default instance from the mule registry
     *
     * @param muleContext the current {@link MuleContext}
     * @return the default instance
     */
    public static TimeSupplier getDefault(MuleContext muleContext)
    {
        TimeSupplier timeSupplier = muleContext.getRegistry().get(OBJECT_TIME_SUPPLIER);
        checkArgument(timeSupplier != null, String.format("Could not find ['%s'] in the registry", OBJECT_TIME_SUPPLIER));

        return timeSupplier;
    }

    /**
     * Returns {@link System#currentTimeMillis()}
     *
     * @return the current time in milliseconds
     */
    @Override
    public Long get()
    {
        return System.currentTimeMillis();
    }
}

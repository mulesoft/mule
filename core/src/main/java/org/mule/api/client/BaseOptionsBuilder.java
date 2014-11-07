/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

public abstract class BaseOptionsBuilder<T extends BaseOptionsBuilder, OptionsType>
{

    private long timeout;

    protected BaseOptionsBuilder()
    {
    }

    public T timeout(final long timeout)
    {
        this.timeout = timeout;
        return (T) this;
    }

    public abstract OptionsType build();

    protected long getTimeout()
    {
        return timeout;
    }
}

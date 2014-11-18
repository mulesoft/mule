/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

/**
 * Most basic options builder that every connector must be able to use for configuration.
 */
public class SimpleOptionsBuilder extends AbstractBaseOptionsBuilder<SimpleOptionsBuilder, OperationOptions>
{

    protected SimpleOptionsBuilder()
    {
    }

    @Override
    public OperationOptions build()
    {
        return new OperationOptions()
        {
            @Override
            public Long getResponseTimeout()
            {
                return SimpleOptionsBuilder.this.getResponseTimeout();
            }

            @Override
            public boolean equals(Object o)
            {
                if (this == o)
                {
                    return true;
                }
                if (!(o instanceof OperationOptions))
                {
                    return false;
                }

                OperationOptions that = (OperationOptions) o;

                Long responseTimeout = getResponseTimeout();
                if (responseTimeout != null ? !responseTimeout.equals(that.getResponseTimeout()) : that.getResponseTimeout() != null)
                {
                    return false;
                }

                return true;
            }

            @Override
            public int hashCode()
            {
                final Long responseTimeout = getResponseTimeout();
                return responseTimeout != null ? responseTimeout.hashCode() : 0;
            }
        };
    }

    /**
     * Factory method for the builder.
     *
     * @return a new options builder
     */
    public static SimpleOptionsBuilder newOptions()
    {
        return new SimpleOptionsBuilder();
    }

}

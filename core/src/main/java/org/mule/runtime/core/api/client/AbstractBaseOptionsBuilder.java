/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

/**
 * Base options for every operation.
 *
 * @param <BuilderType> builder class
 * @param <OptionsType> options type that this builder creates
 */
public abstract class AbstractBaseOptionsBuilder<BuilderType extends AbstractBaseOptionsBuilder, OptionsType> implements OperationOptionsConfig<BuilderType>
{

    private Long responseTimeout;

    protected AbstractBaseOptionsBuilder()
    {
    }

    public BuilderType responseTimeout(final long timeout)
    {
        this.responseTimeout = timeout;
        return (BuilderType) this;
    }

    /**
     * @return the options object holding all the configuration.
     */
    public abstract OptionsType build();

    /**
     * @return configured timeout. null if no timeout was configured
     */
    protected Long getResponseTimeout()
    {
        return responseTimeout;
    }
}

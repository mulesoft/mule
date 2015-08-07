/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

import org.mule.client.SimpleOptions;

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
        return new SimpleOptions(getResponseTimeout());
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

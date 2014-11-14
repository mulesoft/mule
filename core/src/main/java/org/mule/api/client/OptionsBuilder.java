/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.client;

public class OptionsBuilder extends BaseOptionsBuilder<OptionsBuilder, Options>
{

    protected OptionsBuilder()
    {
    }

    @Override
    public Options build()
    {
        return new Options()
        {
            @Override
            public Long getResponseTimeout()
            {
                return getTimeout();
            }
        };
    }

    public static OptionsBuilder newOptions()
    {
        return new OptionsBuilder();
    }

}

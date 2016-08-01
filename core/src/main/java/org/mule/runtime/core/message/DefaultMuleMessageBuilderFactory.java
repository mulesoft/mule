/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import org.mule.runtime.api.message.AbstractMuleMessageBuilderFactory;
import org.mule.runtime.core.api.MuleMessage;

/**
 *
 */
public class DefaultMuleMessageBuilderFactory extends AbstractMuleMessageBuilderFactory
{

    private static DefaultMuleMessageBuilderFactory INSTANCE = new DefaultMuleMessageBuilderFactory();

    public static DefaultMuleMessageBuilderFactory getInstance()
    {
        return INSTANCE;
    }

    @Override
    public MuleMessage.Builder create()
    {
        return new DefaultMuleMessageBuilder();
    }

    @Override
    public MuleMessage.Builder create(org.mule.runtime.api.message.MuleMessage message)
    {
        return new DefaultMuleMessageBuilder(message);
    }

    public MuleMessage.Builder create(MuleMessage message)
    {
        return new DefaultMuleMessageBuilder(message);
    }

}

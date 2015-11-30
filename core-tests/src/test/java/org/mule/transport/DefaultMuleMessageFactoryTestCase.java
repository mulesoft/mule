/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.transport.MuleMessageFactory;

public class DefaultMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    public DefaultMuleMessageFactoryTestCase()
    {
        super();
        runUnsuppoprtedTransportMessageTest = false;
    }
    
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new DefaultMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage()
    {
        return TEST_MESSAGE;
    }
}



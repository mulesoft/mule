/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.rmi.RmiMuleMessageFactory;

public class RmiMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    public RmiMuleMessageFactoryTestCase()
    {
        super();
        runUnsuppoprtedTransportMessageTest = false;
    }
    
    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new RmiMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        return TEST_MESSAGE;
    }
}

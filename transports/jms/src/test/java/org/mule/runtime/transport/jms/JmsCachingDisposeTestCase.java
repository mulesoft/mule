/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class JmsCachingDisposeTestCase extends AbstractBrokerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jms-caching-dispose-config.xml";
    }

    @Test
    public void closesConnectionWhenMuleContextIsDisposed() throws Exception
    {
        assertEquals(1, getConnectionsCount());
        muleContext.dispose();
        assertEquals(0, getConnectionsCount());
    }
}

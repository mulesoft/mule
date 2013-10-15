/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleContext;

/**
 * <code>TestConnector</code> use a mock connector
 */
public class TestConnector2 extends TestConnector
{

    public TestConnector2(MuleContext context)
    {
        super(context);
    }
    
    public String getProtocol()
    {
        return "test2";
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.mock;

import org.apache.axis.AxisEngine;
import org.apache.axis.AxisFault;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.MessageContext;

public class MockAxisEngine extends AxisEngine
{
    public MockAxisEngine(EngineConfiguration config)
    {
        super(config);
    }

    @Override
    public AxisEngine getClientEngine()
    {
        return null;
    }

    public void invoke(MessageContext msgContext) throws AxisFault
    {
        // not implemented
    }
    
    /**
     * open up for testing
     */
    public static void setCurrentMessageContext(MessageContext messageContext) 
    {
        AxisEngine.setCurrentMessageContext(messageContext);
    }
}



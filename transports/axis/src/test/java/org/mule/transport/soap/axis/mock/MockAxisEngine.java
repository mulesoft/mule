/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



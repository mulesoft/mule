/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.test;

import org.mule.api.MuleContext;
import org.mule.transport.jms.JmsConnector;

public class GenericTestJmsConnector extends JmsConnector
{
    private String providerProperty = "NOT_SET";
    
    public GenericTestJmsConnector(MuleContext context)
    {
        super(context);
    }

    public String getProviderProperty()
    {
        return providerProperty;
    }

    public void setProviderProperty(String providerProperty)
    {
        this.providerProperty = providerProperty;
    }
}

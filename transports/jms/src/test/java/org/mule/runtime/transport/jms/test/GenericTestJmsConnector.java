/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.test;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.transport.jms.JmsConnector;

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

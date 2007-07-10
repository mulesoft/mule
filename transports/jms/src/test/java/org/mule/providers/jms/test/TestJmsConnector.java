/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.test;

import org.mule.providers.jms.JmsConnector;
import org.mule.util.object.SimpleObjectFactory;

public class TestJmsConnector extends JmsConnector
{
    private String providerProperty = "NOT_SET";
    
    public TestJmsConnector()
    {
        super();
        setConnectionFactory(new SimpleObjectFactory(TestConnectionFactory.class));
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

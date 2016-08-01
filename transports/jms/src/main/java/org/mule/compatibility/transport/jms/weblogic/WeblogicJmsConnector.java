/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.weblogic;

import org.mule.compatibility.transport.jms.JmsConnector;
import org.mule.runtime.core.api.MuleContext;

/**
 * Weblogic-specific JMS connector.
 */
public class WeblogicJmsConnector extends JmsConnector
{
    /** Constructs a new WeblogicJmsConnector. */
    public WeblogicJmsConnector(MuleContext context)
    {
        super(context);
        setTopicResolver(new WeblogicJmsTopicResolver(this));
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.weblogic;

import org.mule.providers.jms.JmsConnector;

/**
 * Weblogic-specific JMS connector.
 */
public class WeblogicJmsConnector extends JmsConnector
{
    /** Constructs a new WeblogicJmsConnector. */
    public WeblogicJmsConnector()
    {
        setTopicResolver(new WeblogicJmsTopicResolver(this));
    }
}

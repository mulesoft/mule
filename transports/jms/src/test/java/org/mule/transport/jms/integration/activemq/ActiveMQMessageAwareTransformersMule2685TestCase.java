/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration.activemq;

import org.mule.transport.jms.integration.JmsMessageAwareTransformersMule2685TestCase;

/**
 * TODO
 */
public class ActiveMQMessageAwareTransformersMule2685TestCase extends JmsMessageAwareTransformersMule2685TestCase
{

    public ActiveMQMessageAwareTransformersMule2685TestCase()
    {
        super(new ActiveMQJmsConfiguration());
    }
}

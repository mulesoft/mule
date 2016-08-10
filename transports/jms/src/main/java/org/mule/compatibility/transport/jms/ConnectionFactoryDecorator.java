/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms;

import org.mule.runtime.core.api.MuleContext;

import javax.jms.ConnectionFactory;

/**
 * Decorator for the jms ConnectionFactory.
 *
 * Used as mechanism to hook to connection creations or session creations
 * in order to customize behavior.
 */
public interface ConnectionFactoryDecorator
{

    ConnectionFactory decorate(ConnectionFactory connectionFactory, JmsConnector jmsConnector, MuleContext mulecontext);

    boolean appliesTo(ConnectionFactory connectionFactory, MuleContext muleContext);
}

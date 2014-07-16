/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.MessageFactory;

public class PartialStartupRudeMessageProcessor implements MessageProcessor, Startable
{
	@Override
	public MuleEvent process(MuleEvent event) throws MuleException
	{
		return event;
	}
	
	@Override
	public void start() throws MuleException
	{
		throw new MuleException(MessageFactory.createStaticMessage("TOO RUDE!")){};
	}
	
}

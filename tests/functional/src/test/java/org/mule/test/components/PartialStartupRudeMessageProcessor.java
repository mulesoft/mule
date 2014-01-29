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

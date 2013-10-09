/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;
import org.mule.module.bpm.MessageService;
import org.mule.module.bpm.ProcessComponent;

import java.util.Map;

/**
 * Proxy for the message-generation service provided by Mule. The real service gets
 * injected here by the {@link ProcessComponent}.
 */
public class MuleMessageService implements MessageService
{
    private MessageService messageService;

    public void setMessageService(MessageService messageService)
    {
        this.messageService = messageService;
    }

    public MuleMessage generateMessage(String endpoint,
                                       Object payloadObject,
                                       Map messageProperties,
                                       MessageExchangePattern mep) throws Exception
    {
        return messageService.generateMessage(endpoint, payloadObject, messageProperties, mep);
    }
}

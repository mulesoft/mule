/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jbpm;

import org.mule.api.MuleMessage;
import org.mule.transport.bpm.MessageService;
import org.mule.transport.bpm.ProcessConnector;

import java.util.Map;

/**
 * Proxy for the message-generation service provided by Mule.  The real service gets injected here by the {@link ProcessConnector}.
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
                                       boolean synchronous) throws Exception
    {
        return messageService.generateMessage(endpoint, payloadObject, messageProperties, synchronous);
    }
}

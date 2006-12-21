/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.jbpm.msg.mule;

import org.mule.umo.UMOMessage;

import java.util.Map;

import org.jbpm.graph.exe.Token;
import org.jbpm.msg.Message;

public class MuleMessageService implements org.jbpm.msg.MessageService
{

    private static final long serialVersionUID = 1L;

    protected static org.mule.providers.bpm.MessageService proxy;

    public MuleMessageService()
    {
        super();
    }

    public static void setMessageService(org.mule.providers.bpm.MessageService msgService)
    {
        proxy = msgService;
    }

    // TODO This should be replaced by the standard send() method below, which would
    // make Mule the default messaging service within jBpm.
    public UMOMessage generateMessage(String endpoint,
                                      Object payloadObject,
                                      Map messageProperties,
                                      boolean synchronous) throws Exception
    {
        return proxy.generateMessage(endpoint, payloadObject, messageProperties, synchronous);
    }

    public void send(Message message)
    {
        // no-op
    }

    public void suspendMessages(Token token)
    {
        // no-op
    }

    public void resumeMessages(Token token)
    {
        // no-op
    }

    public void close()
    {
        // nop-op
    }

}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.bpm;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleMessage;

import java.util.Map;

/**
 * A message-generation service provided by Mule.  Any BPMS may call this service from
 * its own processes in order to generate Mule messages.  
 */
public interface MessageService 
{
    public MuleMessage generateMessage(String endpoint, Object payloadObject, Map messageProperties, MessageExchangePattern mep) throws Exception;
}

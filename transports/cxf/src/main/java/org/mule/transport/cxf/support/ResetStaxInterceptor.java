/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import java.util.List;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Replaces the XMLStreamReader with a ReversibleXMLStreamReader which caches the xml events so 
 * we can replay them later.
 */
public class ResetStaxInterceptor extends AbstractPhaseInterceptor<Message>
{

    public ResetStaxInterceptor()
    {
        super(Phase.PRE_INVOKE);
        getAfter().add(StaxInInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault
    {
        ReversibleXMLStreamReader reader = message.getContent(ReversibleXMLStreamReader.class);
        reader.reset();
        
        // Replace the message contents because if you're using WSS4J, it leaves the
        // stream pointing to the body, when we want it pointing to the envelope.
        MessageContentsList parameters = new MessageContentsList();
        parameters.add(reader);
        message.setContent(List.class, parameters);
    }
}



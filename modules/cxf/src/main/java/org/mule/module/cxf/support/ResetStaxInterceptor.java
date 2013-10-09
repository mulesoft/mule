/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

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



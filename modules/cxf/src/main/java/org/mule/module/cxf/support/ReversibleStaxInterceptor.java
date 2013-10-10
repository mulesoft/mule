/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * Creates a ReversibleXMLStreamReader to be able to track and replay events from the XMLStreamReader.
 */
public abstract class ReversibleStaxInterceptor extends AbstractPhaseInterceptor<Message>
{
    public ReversibleStaxInterceptor(String phase)
    {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault
    {
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);

        if (reader != null)
        {
            ReversibleXMLStreamReader reversible = new ReversibleXMLStreamReader(reader);
            reversible.setTracking(true);
            message.setContent(XMLStreamReader.class, reversible);
            message.setContent(ReversibleXMLStreamReader.class, reversible);
        }
    }

}

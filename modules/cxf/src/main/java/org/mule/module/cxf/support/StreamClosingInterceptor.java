/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import org.mule.module.xml.stax.DelegateXMLStreamReader;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Replaces the original XMLStreamReader with another one which
 * closes the underlying InputStream.
 */
public class StreamClosingInterceptor extends AbstractPhaseInterceptor<Message>
{
    public StreamClosingInterceptor()
    {
        super(Phase.POST_STREAM);
        addAfter(StaxInInterceptor.class.getName());
    }

    public void handleMessage(final Message message) throws Fault
    {
        XMLStreamReader xsr = message.getContent(XMLStreamReader.class);
        final InputStream is = message.getContent(InputStream.class);
        DelegateXMLStreamReader xsr2 = new DelegateXMLStreamReader(xsr) {

            @Override
            public void close() throws XMLStreamException
            {
                super.close();
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    throw new XMLStreamException(e);
                }
            }
        };
        message.setContent(XMLStreamReader.class, xsr2);
    }
}


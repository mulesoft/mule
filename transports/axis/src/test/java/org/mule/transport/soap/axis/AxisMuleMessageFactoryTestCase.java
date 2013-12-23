/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;
import org.mule.transport.soap.axis.mock.MockAxisEngine;
import org.mule.transport.soap.axis.mock.MockEngineConfiguration;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AxisMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String XML_WITH_HEADERS =
          "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
        + "    <soapenv:Header>"
        + "        <mule:header soapenv:actor=\"http://www.muleumo.org/providers/soap/1.0\" soapenv:mustUnderstand=\"0\" xmlns:mule=\"http://www.muleumo.org/providers/soap/1.0\">"        
        + "            <mule:MULE_REPLYTO>replyTo</mule:MULE_REPLYTO>"
        + "            <mule:MULE_CORRELATION_ID>004a1cf9-3e7e-44b3-9b7f-778fae4fa0d2</mule:MULE_CORRELATION_ID>"
        + "            <mule:MULE_CORRELATION_GROUP_SIZE>42</mule:MULE_CORRELATION_GROUP_SIZE>"
        + "            <mule:MULE_CORRELATION_SEQUENCE>-42</mule:MULE_CORRELATION_SEQUENCE>"
        + "        </mule:header>"
        + "    </soapenv:Header>"
        + "    <soapenv:Body>"
        + "        <echo soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "            <value0 xsi:type=\"soapenc:string\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">"
        + "                Hello"
        + "            </value0>"
        + "        </echo>"
        + "    </soapenv:Body>"
        + "</soapenv:Envelope>";
    
    public AxisMuleMessageFactoryTestCase()
    {
        super();
        runUnsuppoprtedTransportMessageTest = false;
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        setupAxisMessageContext();
    }

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new AxisMuleMessageFactory();
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        return new Message(XML_WITH_HEADERS);
    }

    @Test
    public void testSoapHeaders() throws Exception
    {
        MuleMessageFactory factory = createMuleMessageFactory();
        Object payload = getValidTransportMessage();
        MuleMessage message = factory.create(payload, encoding, muleContext);
        assertEquals(payload, message.getPayload());
        assertEquals("replyTo", message.getReplyTo());
        assertEquals(42, message.getCorrelationGroupSize());
        assertEquals(-42, message.getCorrelationSequence());
        assertEquals("004a1cf9-3e7e-44b3-9b7f-778fae4fa0d2", message.getCorrelationId());
    }
    
    private void setupAxisMessageContext()
    {
        EngineConfiguration configuration = new MockEngineConfiguration();
        MockAxisEngine engine = new MockAxisEngine(configuration);
        MessageContext messageContext = new MessageContext(engine);
        MockAxisEngine.setCurrentMessageContext(messageContext);
        
        Message soapMessage = new Message(XML_WITH_HEADERS);
        messageContext.setMessage(soapMessage);        
    }
}

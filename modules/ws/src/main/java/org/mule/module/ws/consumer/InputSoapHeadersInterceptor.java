/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.cxf.CxfConstants;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;

/**
 * CXF interceptor that adds Soap headers to the SoapMessage based on outbound properties
 * from the Mule message that start with the soap prefix.
 */
public class InputSoapHeadersInterceptor extends AbstractSoapInterceptor
{

    private final MuleContext muleContext;

    public InputSoapHeadersInterceptor(MuleContext muleContext)
    {
        super(Phase.PRE_PROTOCOL);
        this.muleContext = muleContext;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault
    {
        Map<String, Object> invocationContext = (Map<String, Object>) message.get(Message.INVOCATION_CONTEXT);
        Map<String, Object> requestContext = (Map<String, Object>) invocationContext.get(Client.REQUEST_CONTEXT);

        /* Outbound properties are copied to the CXF request context by the CxfOutboundMessageProcessor. As CXF
         * generates the message lazily, by the time this interceptor is executed the outbound SOAP headers are
         * already removed from the Mule message, so we need to read them from the request context. */

        for (String outboundProperty : requestContext.keySet())
        {
            if (outboundProperty.startsWith(WSConsumer.SOAP_HEADERS_PROPERTY_PREFIX))
            {
                Object value = requestContext.get(outboundProperty);
                Transformer transformer = null;

                try
                {
                    transformer = muleContext.getRegistry().lookupTransformer(DataTypeFactory.createFromObject(value),
                                                                              new SimpleDataType(Document.class));
                }
                catch (TransformerException e)
                {
                    MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
                    throw new Fault(new TransformerMessagingException(
                            CoreMessages.createStaticMessage("Cannot find transformer to convert outbound property %s to XML",
                                                             outboundProperty), event, transformer, e.getCause()));
                }

                try
                {
                    Document document = (Document) transformer.transform(value);

                    // This QName is required by the SoapHeader but it is not used.
                    QName qname = new QName(null, document.getDocumentElement().getTagName());

                    message.getHeaders().add(new SoapHeader(qname, document.getDocumentElement()));
                }
                catch (TransformerException e)
                {
                    MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);

                    throw new Fault(new TransformerMessagingException(
                            CoreMessages.createStaticMessage("Outbound property %s contains an invalid XML string",
                                                             outboundProperty), event, transformer, e.getCause()));
                }
            }
        }
    }
}

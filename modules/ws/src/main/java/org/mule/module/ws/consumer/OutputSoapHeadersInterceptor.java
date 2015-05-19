/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.cxf.CxfConstants;
import org.mule.transformer.types.DataTypeFactory;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that adds inbound properties to the Mule message based on the SOAP headers
 * received in the response.
 */
public class OutputSoapHeadersInterceptor extends AbstractSoapInterceptor
{

    private final MuleContext muleContext;

    public OutputSoapHeadersInterceptor(MuleContext muleContext)
    {
        super(Phase.PRE_PROTOCOL);
        this.muleContext = muleContext;
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);

        if (event == null || event instanceof NonBlockingVoidMuleEvent)
        {
            return;
        }

        for (Header header : message.getHeaders())
        {
            if (header instanceof SoapHeader)
            {
                Transformer transformer = null;

                try
                {
                    DataType sourceType = DataTypeFactory.createFromObject(header.getObject());
                    transformer = muleContext.getRegistry().lookupTransformer(sourceType, DataType.STRING_DATA_TYPE);

                    String key = WSConsumer.SOAP_HEADERS_PROPERTY_PREFIX + header.getName().getLocalPart();
                    String value = (String) transformer.transform(header.getObject());

                    event.getMessage().setProperty(key, value, PropertyScope.INBOUND);
                }
                catch (TransformerException e)
                {
                    throw new Fault(new TransformerMessagingException(
                            CoreMessages.createStaticMessage("Cannot parse content of SOAP header %s in the response",
                                                             header.getName().getLocalPart()), event, transformer, e.getCause()));
                }
            }
        }
    }
}
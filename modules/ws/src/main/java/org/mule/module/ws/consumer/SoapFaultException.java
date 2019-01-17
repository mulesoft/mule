/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

import static org.mule.config.DefaultMuleConfiguration.isVerboseExceptions;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.w3c.dom.Element;

/**
 * Exception thrown by the Web Services Consumer when processing a SOAP fault.
 * The exception contains the details about the fault.
 */
public class SoapFaultException extends MessagingException
{
    private final QName faultCode;
    private final QName subCode;
    private final Element detail;

    @Deprecated
    public SoapFaultException(MuleEvent event, QName faultCode, QName subCode, String message, Element detail, MessageProcessor failingMessageProcessor)
    {
        super(CoreMessages.createStaticMessage(message), event, failingMessageProcessor);
        this.faultCode = faultCode;
        this.subCode = subCode;
        this.detail = detail;
    }

    public SoapFaultException(MuleEvent event, SoapFault soapFault, MessageProcessor failingMessageProcessor)
    {
        super(CoreMessages.createStaticMessage(soapFault.getMessage()), event, soapFault, failingMessageProcessor);
        this.faultCode = soapFault.getFaultCode();
        this.subCode = soapFault.getSubCode();
        this.detail = soapFault.getDetail();
    }

    public QName getFaultCode()
    {
        return faultCode;
    }

    public QName getSubCode()
    {
        return subCode;
    }

    public Element getDetail()
    {
        return detail;
    }
    
    @Override
    protected String generateMessage(Message message)
    {
        if (message != null && !isVerboseExceptions())
        {
            return message.getMessage();
        }
        
        return super.generateMessage(message);
    }
}

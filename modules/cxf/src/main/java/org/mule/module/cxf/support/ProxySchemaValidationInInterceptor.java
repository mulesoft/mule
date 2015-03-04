/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.StaxValidationManager;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.ServiceInfo;

public class ProxySchemaValidationInInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(ProxySchemaValidationInInterceptor.class);

    private Endpoint endpoint;
    private ServiceInfo service;
    private Bus bus;
    
    public ProxySchemaValidationInInterceptor(Bus bus, Endpoint endpoint, ServiceInfo service) {
        super(Phase.READ);
        this.bus = bus;
        this.endpoint = endpoint;
        this.service = service;
        addBefore(StartBodyInterceptor.class.getName());
        addAfter(ReadHeadersInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        
        // if we're in proxy envelope mode, find the underlying stream reader before performing validation
        if (xmlReader instanceof ReversibleXMLStreamReader) {
            xmlReader = ((ReversibleXMLStreamReader) xmlReader).getDelegateReader();
        }
        
        try {
            setSchemaInMessage(message, xmlReader);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("SCHEMA_ERROR", LOG), 
                            e);
        }
    }
    
    private void setSchemaInMessage(Message message, XMLStreamReader reader) throws XMLStreamException  {
        Object en = message.getContextualProperty(org.apache.cxf.message.Message.SCHEMA_VALIDATION_ENABLED);
        if (Boolean.TRUE.equals(en) || "true".equals(en)) {
            StaxValidationManager mgr = bus.getExtension(StaxValidationManager.class);
            if (mgr != null) {
                mgr.setupValidation(reader, endpoint, service);
            }
        }
    }
}

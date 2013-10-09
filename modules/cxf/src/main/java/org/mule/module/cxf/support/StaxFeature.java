/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;

/**
 * Configures the StAX XMLInputFactory and XMLOutputFactory which CXF uses.
 * 
 */
public class StaxFeature extends AbstractFeature {
    private String xmlInputFactory;
    private String xmlOutputFactory;
    
    @Override
    public void initialize(Client client, Bus bus) {
        Service service = client.getEndpoint().getService();
        
        setProperties(service);
    }

    private void setProperties(Service service) {
        if (xmlInputFactory != null) {
            service.put(XMLInputFactory.class.getName(), xmlInputFactory);
        }
        
        if (xmlOutputFactory != null) {
            service.put(XMLOutputFactory.class.getName(), xmlOutputFactory);
        }
    }

    @Override
    public void initialize(Server server, Bus bus) {
        Service service = server.getEndpoint().getService();
        
        setProperties(service);
    }

    @Override
    public void initialize(Bus bus) {
        AbstractPhaseInterceptor<Message> in = new AbstractPhaseInterceptor<Message>(Phase.RECEIVE) {
            public void handleMessage(Message message) throws Fault {
                if (xmlInputFactory != null) {
                    message.put(XMLInputFactory.class.getName(), xmlInputFactory);
                }
            }
        };
        
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        
        AbstractPhaseInterceptor<Message> out = new AbstractPhaseInterceptor<Message>(Phase.SETUP) {
            public void handleMessage(Message message) throws Fault {
                if (xmlOutputFactory != null) {
                    message.put(XMLOutputFactory.class.getName(), xmlOutputFactory);
                }
            }
        };
        
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
    }

    public String getXmlInputFactory() {
        return xmlInputFactory;
    }

    public void setXmlInputFactory(String xmlInputFactory) {
        this.xmlInputFactory = xmlInputFactory;
    }

    public String getXmlOutputFactory() {
        return xmlOutputFactory;
    }

    public void setXmlOutputFactory(String xmlOutputFactory) {
        this.xmlOutputFactory = xmlOutputFactory;
    }

}

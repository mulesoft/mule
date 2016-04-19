/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.builder;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.CxfInboundMessageProcessor;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.CxfPayloadToArguments;
import org.mule.runtime.module.cxf.config.WsSecurity;
import org.mule.runtime.module.cxf.support.MuleHeadersInInterceptor;
import org.mule.runtime.module.cxf.support.MuleHeadersOutInterceptor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

public abstract class AbstractOutboundMessageProcessorBuilder 
    implements MessageProcessorBuilder, MuleContextAware
{
    protected Client client;
    protected String defaultMethodName;
    protected Method defaultMethod;

    protected CxfConfiguration configuration;
    protected List<Interceptor<? extends Message>> inInterceptors;
    protected List<Interceptor<? extends Message>> inFaultInterceptors;
    protected List<Interceptor<? extends Message>> outInterceptors;
    protected List<Interceptor<? extends Message>> outFaultInterceptors;
    protected DataBinding databinding;
    protected List<AbstractFeature> features;
    protected String wsdlLocation;
    protected boolean mtomEnabled;
    protected String soapVersion;
    protected boolean enableMuleSoapHeaders = true;
    protected CxfPayloadToArguments payloadToArguments = CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER;
    protected Map<String,Object> properties = new HashMap<String, Object>();
    protected MuleContext muleContext;
    protected String address;
    protected String operation;
    protected String decoupledEndpoint;

    private WsSecurity wsSecurity;

    @Override
    public CxfOutboundMessageProcessor build() throws MuleException
    {
        if (muleContext == null) 
        {
            throw new IllegalStateException("MuleContext must be supplied.");
        }
        
        if (configuration == null)
        {
            configuration = CxfConfiguration.getConfiguration(muleContext);
        }
        
        // set the thread default bus so the JAX-WS Service implementation (or other bits of CXF code
        // which I don't know about, but may depend on it) can use it when creating a Client -- DD
        BusFactory.setThreadDefaultBus(getBus());
       
        try
        {
            client = createClient();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }

        addInterceptors(client.getInInterceptors(), inInterceptors);
        addInterceptors(client.getInFaultInterceptors(), inFaultInterceptors);
        addInterceptors(client.getOutInterceptors(), outInterceptors);
        addInterceptors(client.getOutFaultInterceptors(), outFaultInterceptors);

        client.setThreadLocalRequestContext(true);

        if(wsSecurity != null && wsSecurity.getConfigProperties() != null && !wsSecurity.getConfigProperties().isEmpty())
        {
            client.getOutInterceptors().add(new WSS4JOutInterceptor(wsSecurity.getConfigProperties()));
        }

        configureClient(client);
        
        if (features != null)
        {
            for (AbstractFeature f : features)
            {
                f.initialize(client, getBus());
            }
        }

        if (mtomEnabled)
        {
            client.getEndpoint().put(Message.MTOM_ENABLED, mtomEnabled);
        }

        addMuleInterceptors();
        
        CxfOutboundMessageProcessor processor = createMessageProcessor();
        processor.setOperation(operation);
        configureMessageProcessor(processor);
        processor.setPayloadToArguments(payloadToArguments);

        if (decoupledEndpoint != null)
        {
            processor.setDecoupledEndpoint(decoupledEndpoint);

            CxfInboundMessageProcessor cxfInboundMP = new CxfInboundMessageProcessor();
            cxfInboundMP.setMuleContext(muleContext);
            cxfInboundMP.setBus(getBus());

            List<MessageProcessor> mps = new ArrayList<MessageProcessor>();
            mps.add(cxfInboundMP);

            EndpointBuilder ep = muleContext.getEndpointFactory().getEndpointBuilder(decoupledEndpoint);

            Flow flow = new Flow("decoupled-" + ep.toString(), muleContext);
            flow.setMessageProcessors(mps);
            flow.setMessageSource(ep.buildInboundEndpoint());
            muleContext.getRegistry().registerObject(flow.getName(), flow);
        }

        processor.setMimeType(getMimeType());
        
        return processor;
    }

    protected CxfOutboundMessageProcessor createMessageProcessor()
    {
        CxfOutboundMessageProcessor processor = new CxfOutboundMessageProcessor(client);
        processor.setMuleContext(muleContext);
        return processor;
    }

    protected void configureMessageProcessor(CxfOutboundMessageProcessor processor)
    {
    }

    protected void configureClient(Client client)
    {
    }

    protected Bus getBus()
    {
        return configuration.getCxfBus();
    }

    protected abstract Client createClient() throws CreateException, Exception;

    public Client getClient()
    {
        return client;
    }

    private void addInterceptors(List<Interceptor<? extends Message>> col, List<Interceptor<? extends Message>> supplied)
    {
        if (supplied != null) 
        {
            col.addAll(supplied);
        }
    }
    
    protected String getAddress()
    {
        if (address == null) 
        {
            // dummy URL for client builder
            return "http://host";
        }
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    protected void createClientFromLocalServer() throws Exception
    {
        // template method
    }

    protected void addMuleInterceptors()
    {

        if (enableMuleSoapHeaders && !configuration.isEnableMuleSoapHeaders())
        {
            client.getInInterceptors().add(new MuleHeadersInInterceptor());
            client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
            client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public DataBinding getDatabinding()
    {
        return databinding;
    }

    public void setDatabinding(DataBinding databinding)
    {
        this.databinding = databinding;
    }

    public boolean isMtomEnabled()
    {
        return mtomEnabled;
    }

    public void setMtomEnabled(boolean mtomEnabled)
    {
        this.mtomEnabled = mtomEnabled;
    }

    public void setSoapVersion(String soapVersion)
    {
        this.soapVersion = soapVersion;
    }

    public String getSoapVersion()
    {
        return soapVersion;
    }
    
    public List<Interceptor<? extends Message>> getInInterceptors()
    {
        return inInterceptors;
    }

    public void setInInterceptors(List<Interceptor<? extends Message>> inInterceptors)
    {
        this.inInterceptors = inInterceptors;
    }

    public List<Interceptor<? extends Message>> getInFaultInterceptors()
    {
        return inFaultInterceptors;
    }

    public void setInFaultInterceptors(List<Interceptor<? extends Message>> inFaultInterceptors)
    {
        this.inFaultInterceptors = inFaultInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutInterceptors()
    {
        return outInterceptors;
    }

    public void setOutInterceptors(List<Interceptor<? extends Message>> outInterceptors)
    {
        this.outInterceptors = outInterceptors;
    }

    public List<Interceptor<? extends Message>> getOutFaultInterceptors()
    {
        return outFaultInterceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> outFaultInterceptors)
    {
        this.outFaultInterceptors = outFaultInterceptors;
    }

    public List<AbstractFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<AbstractFeature> features)
    {
        this.features = features;
    }
    
    public String getWsdlLocation()
    {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation)
    {
        this.wsdlLocation = wsdlLocation;
    }
    
    public CxfConfiguration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(CxfConfiguration configuration)
    {
        this.configuration = configuration;
    }

    public boolean isEnableMuleSoapHeaders()
    {
        return enableMuleSoapHeaders;
    }

    public void setEnableMuleSoapHeaders(boolean enableMuleSoapHeaders)
    {
        this.enableMuleSoapHeaders = enableMuleSoapHeaders;
    }

    public CxfPayloadToArguments getPayloadToArguments()
    {
        return payloadToArguments;
    }

    public void setPayloadToArguments(CxfPayloadToArguments payloadToArguments)
    {
        this.payloadToArguments = payloadToArguments;
    }
    
    public Map<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(Map<String, Object> properties)
    {
        this.properties = properties;
    }

    public void setAddProperties(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }

    public String getDecoupledEndpoint()
    {
        return decoupledEndpoint;
    }

    public void setDecoupledEndpoint(String decoupledEndpoint)
    {
        this.decoupledEndpoint = decoupledEndpoint;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public void setWsSecurity(WsSecurity wsSecurity)
    {
        this.wsSecurity = wsSecurity;
    }

    protected String getMimeType()
    {
        return MimeTypes.ANY;
    }

}

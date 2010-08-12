/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.builder;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.transport.cxf.CxfConfiguration;
import org.mule.transport.cxf.CxfOutboundMessageProcessor;
import org.mule.transport.cxf.CxfPayloadToArguments;
import org.mule.transport.cxf.support.MuleHeadersInInterceptor;
import org.mule.transport.cxf.support.MuleHeadersOutInterceptor;
import org.mule.transport.cxf.support.MuleProtocolHeadersOutInterceptor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

public abstract class AbstractOutboundMessageProcessorBuilder 
    implements MessageProcessorBuilder, MuleContextAware
{
    protected Client client;
    protected String defaultMethodName;
    protected Method defaultMethod;

    protected CxfConfiguration configuration;
    protected List<Interceptor> inInterceptors;
    protected List<Interceptor> inFaultInterceptors;
    protected List<Interceptor> outInterceptors;
    protected List<Interceptor> outFaultInterceptors;
    protected DataBinding databinding;
    protected List<AbstractFeature> features;
    protected String wsdlLocation;
    protected boolean mtomEnabled;
    protected boolean enableMuleSoapHeaders = true;
    protected CxfPayloadToArguments payloadToArguments = CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER;
    protected Map<String,Object> properties;
    protected MuleContext muleContext;
    protected String address;
    protected String operation;
    
    @SuppressWarnings("unchecked")
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
        return processor;
    }

    protected CxfOutboundMessageProcessor createMessageProcessor()
    {
        CxfOutboundMessageProcessor processor = new CxfOutboundMessageProcessor(client);
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

    @SuppressWarnings("unchecked")
    private void addInterceptors(List<Interceptor> col, List<Interceptor> supplied)
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
    }

    protected void addMuleInterceptors()
    {
        if (enableMuleSoapHeaders)
        {
            client.getInInterceptors().add(new MuleHeadersInInterceptor());
            client.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
            client.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            client.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }
        client.getOutInterceptors().add(new MuleProtocolHeadersOutInterceptor());
        client.getOutFaultInterceptors().add(new MuleProtocolHeadersOutInterceptor());
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
    
    public List<Interceptor> getInInterceptors()
    {
        return inInterceptors;
    }

    public void setInInterceptors(List<Interceptor> inInterceptors)
    {
        this.inInterceptors = inInterceptors;
    }

    public List<Interceptor> getInFaultInterceptors()
    {
        return inFaultInterceptors;
    }

    public void setInFaultInterceptors(List<Interceptor> inFaultInterceptors)
    {
        this.inFaultInterceptors = inFaultInterceptors;
    }

    public List<Interceptor> getOutInterceptors()
    {
        return outInterceptors;
    }

    public void setOutInterceptors(List<Interceptor> outInterceptors)
    {
        this.outInterceptors = outInterceptors;
    }

    public List<Interceptor> getOutFaultInterceptors()
    {
        return outFaultInterceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor> outFaultInterceptors)
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

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }
    
}

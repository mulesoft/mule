/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.builder;

import org.mule.AbstractAnnotatedObject;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.service.ServiceAware;
import org.mule.module.cxf.CxfConfiguration;
import org.mule.module.cxf.CxfInboundMessageProcessor;
import org.mule.module.cxf.MuleInvoker;
import org.mule.module.cxf.config.WsSecurity;
import org.mule.module.cxf.support.CxfUtils;
import org.mule.module.cxf.support.MuleHeadersInInterceptor;
import org.mule.module.cxf.support.MuleHeadersOutInterceptor;
import org.mule.module.cxf.support.MuleServiceConfiguration;
import org.mule.module.cxf.support.WSDLQueryHandler;
import org.mule.transformer.types.MimeTypes;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.cxf.Bus;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.transports.http.QueryHandler;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.ws.security.handler.WSHandlerConstants;

/**
 * An abstract builder for CXF services. It handles all common operations such
 * as interceptor configuration, mule header enabling, etc. Subclasses can extend
 * this and control how the Server is created and how the {@link CxfInboundMessageProcessor}
 * is configured.
 */
public abstract class AbstractInboundMessageProcessorBuilder extends AbstractAnnotatedObject implements MuleContextAware, MessageProcessorBuilder
{
    private CxfConfiguration configuration;
    private Server server;
    private boolean enableMuleSoapHeaders = true;
    private String wsdlLocation;
    private String bindingId;
    private String mtomEnabled;
    private String soapVersion;
    private String service;
    private String namespace;
    private List<AbstractFeature> features;
    private List<Interceptor<? extends Message>> inInterceptors = new CopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> inFaultInterceptors = new CopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outInterceptors = new CopyOnWriteArrayList<Interceptor<? extends Message>>();
    private List<Interceptor<? extends Message>> outFaultInterceptors = new CopyOnWriteArrayList<Interceptor<? extends Message>>();
    protected MuleContext muleContext;
    private String port;
    private Map<String,Object> properties = new HashMap<String, Object>();
    private boolean validationEnabled;
    private List<String> schemaLocations;
    private WsSecurity wsSecurity;

    @Override
    public CxfInboundMessageProcessor build() throws MuleException
    {
        if (muleContext == null)
        {
            throw new IllegalStateException("MuleContext must be supplied.");
        }

        if (configuration == null)
        {
            configuration = CxfConfiguration.getConfiguration(muleContext);
        }

        if (configuration == null)
        {
            throw new IllegalStateException("A CxfConfiguration object must be supplied.");
        }

        ServerFactoryBean sfb;
        try
        {
            sfb = createServerFactory();
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }

        // The binding - i.e. SOAP, XML, HTTP Binding, etc
        if (bindingId != null)
        {
            sfb.setBindingId(bindingId);
        }

        if (features != null)
        {
            sfb.getFeatures().addAll(features);
        }

        if (mtomEnabled != null)
        {
            properties.put("mtom-enabled", mtomEnabled);
            properties.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, true);
        }

        if (inInterceptors != null)
        {
            sfb.getInInterceptors().addAll(inInterceptors);
        }

        // If some correcting measure needs to be taken due to user-defined in-interceptors
        addInInterceptorCorrectingInterceptors(sfb);

        if (inFaultInterceptors != null)
        {
            sfb.getInFaultInterceptors().addAll(inFaultInterceptors);
        }

        if (outInterceptors != null)
        {
            sfb.getOutInterceptors().addAll(outInterceptors);
        }

        if (outFaultInterceptors != null)
        {
            sfb.getOutFaultInterceptors().addAll(outFaultInterceptors);
        }

        if (enableMuleSoapHeaders && !configuration.isEnableMuleSoapHeaders())
        {
            sfb.getInInterceptors().add(new MuleHeadersInInterceptor());
            sfb.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
            sfb.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            sfb.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }

        setSecurityConfig(sfb);

        String address = getAddress();
        address = CxfUtils.mapUnsupportedSchemas(address);
        sfb.setAddress(address); // dummy URL for CXF

        if (wsdlLocation != null)
        {
            sfb.setWsdlURL(wsdlLocation);
        }

        sfb.setSchemaLocations(schemaLocations);

        ReflectionServiceFactoryBean svcFac = sfb.getServiceFactory();
        initServiceFactory(svcFac);

        CxfInboundMessageProcessor processor = new CxfInboundMessageProcessor();
        processor.setMuleContext(muleContext);
        configureMessageProcessor(sfb, processor);
        sfb.setStart(false);

        Bus bus = configuration.getCxfBus();
        sfb.setBus(bus);
        svcFac.setBus(bus);

        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer)
        {
            configurer.configureBean(svcFac.getEndpointName().toString(), sfb);
        }

        if (validationEnabled)
        {
            properties.put("schema-validation-enabled", "true");
        }

        // If there's a soapVersion defined then the corresponding bindingId will be set
        if(soapVersion != null)
        {
            sfb.setBindingId(CxfUtils.getBindingIdForSoapVersion(soapVersion));
        }

        sfb.setProperties(properties);
        sfb.setInvoker(createInvoker(processor));

        server = sfb.create();

        CxfUtils.removeInterceptor(server.getEndpoint().getService().getInInterceptors(), OneWayProcessorInterceptor.class.getName());
        configureServer(server);

        processor.setBus(sfb.getBus());
        processor.setServer(server);
        processor.setProxy(isProxy());
        processor.setWSDLQueryHandler(getWSDLQueryHandler());
        processor.setMimeType(getMimeType());

        return processor;
    }

    protected void addInInterceptorCorrectingInterceptors(ServerFactoryBean sfb)
    {
        // Default implementation does nothing.
    }

    protected String getMimeType()
    {
        return MimeTypes.ANY;
    }

    protected QueryHandler getWSDLQueryHandler()
    {
        return new WSDLQueryHandler(configuration.getCxfBus());
    }

    protected Invoker createInvoker(CxfInboundMessageProcessor processor)
    {
        return new MuleInvoker(processor, getServiceClass());
    }

    protected void configureServer(Server server2)
    {
        // template method
    }

    protected abstract Class<?> getServiceClass();

    protected void configureMessageProcessor(ServerFactoryBean sfb, CxfInboundMessageProcessor processor)
    {
        // template method
    }

    protected abstract ServerFactoryBean createServerFactory() throws Exception;

    protected String getAddress()
    {
        return "http://internalMuleCxfRegistry/" + hashCode();
    }

    /**
     * This method configures the {@link ReflectionServiceFactoryBean}.
     */
    private void initServiceFactory(ReflectionServiceFactoryBean svcFac)
    {
        addIgnoredMethods(svcFac, Callable.class.getName());
        addIgnoredMethods(svcFac, Initialisable.class.getName());
        addIgnoredMethods(svcFac, Disposable.class.getName());
        addIgnoredMethods(svcFac, ServiceAware.class.getName());

        svcFac.getServiceConfigurations().add(0, new MuleServiceConfiguration(this));

        svcFac.setServiceClass(getServiceClass());
        for (AbstractServiceConfiguration c : svcFac.getServiceConfigurations())
        {
            c.setServiceFactory(svcFac);
        }
    }

    public void addIgnoredMethods(ReflectionServiceFactoryBean svcFac, String className)
    {
        try
        {
            Class<?> c = ClassUtils.loadClass(className, getClass());
            for (int i = 0; i < c.getMethods().length; i++)
            {
                svcFac.getIgnoredMethods().add(c.getMethods()[i]);
            }
        }
        catch (ClassNotFoundException e)
        {
            // can be ignored.
        }
    }

    private void setSecurityConfig(ServerFactoryBean sfb)
    {
        if(wsSecurity != null)
        {
            if(wsSecurity.getCustomValidator() != null && !wsSecurity.getCustomValidator().isEmpty())
            {
                for(Map.Entry<String, Object> entry : wsSecurity.getCustomValidator().entrySet())
                {
                    properties.put(entry.getKey(), entry.getValue());
                }
            }
            if(wsSecurity.getSecurityManager() != null)
            {
                properties.put(SecurityConstants.USERNAME_TOKEN_VALIDATOR, wsSecurity.getSecurityManager());
            }
            if(wsSecurity.getConfigProperties() != null && !wsSecurity.getConfigProperties().isEmpty())
            {
                sfb.getInInterceptors().add(new WSS4JInInterceptor(wsSecurity.getConfigProperties()));

                // CXF changed the way it validates SAML subject confirmation from 2.5.x to 2.7.x
                // see https://issues.apache.org/jira/browse/CXF-4655
                // In order to keep backwards compatibility we use the previous approach
                String actionProperty = (String) wsSecurity.getConfigProperties().get(WSHandlerConstants.ACTION);
                if (!StringUtils.isEmpty(actionProperty) && actionProperty.contains(WSHandlerConstants.SAML_TOKEN_UNSIGNED))
                {
                    properties.put("ws-security.validate.saml.subject.conf", false);
                }
            }
        }
    }


    public Server getServer()
    {
        return server;
    }

    public abstract boolean isProxy();

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

    public String getWsdlLocation()
    {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlUrl)
    {
        this.wsdlLocation = wsdlUrl;
    }

    public String getBindingId()
    {
        return bindingId;
    }

    public void setBindingId(String bindingId)
    {
        this.bindingId = bindingId;
    }

    public void setSoapVersion(String soapVersion)
    {
        this.soapVersion = soapVersion;
    }

    public String getSoapVersion()
    {
        return soapVersion;
    }

    public String getMtomEnabled()
    {
        return mtomEnabled;
    }

    public void setMtomEnabled(String mtomEnabled)
    {
        this.mtomEnabled = mtomEnabled;
    }

    public String getService()
    {
        return service;
    }

    public void setService(String name)
    {
        this.service = name;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public List<AbstractFeature> getFeatures()
    {
        return features;
    }

    public void setFeatures(List<AbstractFeature> features)
    {
        this.features = features;
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

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String endpoint)
    {
        this.port = endpoint;
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

    public boolean isValidationEnabled()
    {
        return validationEnabled;
    }

    public void setValidationEnabled(boolean validationEnabled)
    {
        this.validationEnabled = validationEnabled;
    }

    public List<String> getSchemaLocations()
    {
        return schemaLocations;
    }

    public void setSchemaLocations(List<String> schemaLocations)
    {
        this.schemaLocations = schemaLocations;
    }

    public void setWsSecurity(WsSecurity wsSecurity)
    {
        this.wsSecurity = wsSecurity;
    }

}

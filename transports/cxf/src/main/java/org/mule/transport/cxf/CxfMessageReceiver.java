/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.component.JavaComponent;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.cxf.i18n.CxfMessages;
import org.mule.transport.cxf.support.CopyAttachmentInInterceptor;
import org.mule.transport.cxf.support.CopyAttachmentOutInterceptor;
import org.mule.transport.cxf.support.CxfUtils;
import org.mule.transport.cxf.support.MuleHeadersInInterceptor;
import org.mule.transport.cxf.support.MuleProtocolHeadersOutInterceptor;
import org.mule.transport.cxf.support.OutputPayloadInterceptor;
import org.mule.transport.cxf.support.ProxyService;
import org.mule.transport.cxf.support.ProxyServiceFactoryBean;
import org.mule.transport.cxf.support.ResetStaxInterceptor;
import org.mule.transport.cxf.support.ReversibleStaxInInterceptor;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;

/**
 * Create a CXF service. All messages for the service will be sent to the Mule bus a
 * la the MuleInvoker.
 */
public class CxfMessageReceiver extends AbstractMessageReceiver
{

    protected CxfConnector connector;
    private Server server;
    private boolean proxy;
    private boolean applySecurityToProtocol;
    private boolean applyTransformersToProtocol;
    private boolean applyFiltersToProtocol;
    private boolean enableHeaders;
    private String payload;
    
    public CxfMessageReceiver(Connector connector, Service service, InboundEndpoint Endpoint)
        throws CreateException
    {
        super(connector, service, Endpoint);
        this.connector = (CxfConnector) connector;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        
        try
        {
            Map endpointProps = getEndpoint().getProperties();
            String wsdlUrl = (String) endpointProps.get(CxfConstants.WSDL_LOCATION);
            String bindingId = (String) endpointProps.get(CxfConstants.BINDING_ID);
            String frontend = (String) endpointProps.get(CxfConstants.FRONTEND);
            String serviceClassName = (String) endpointProps.get(CxfConstants.SERVICE_CLASS);
            String mtomEnabled = (String) endpointProps.get(CxfConstants.MTOM_ENABLED);
            List<DataBinding> databinding = (List<DataBinding>) endpointProps.get(CxfConstants.DATA_BINDING);
            List<AbstractFeature> features = (List<AbstractFeature>) endpointProps.get(CxfConstants.FEATURES);
            String proxyStr = (String) endpointProps.get(CxfConstants.PROXY);
            payload = (String) endpointProps.get(CxfConstants.PAYLOAD);
            
            applyFiltersToProtocol = isTrue((String) endpointProps.get(CxfConstants.APPLY_FILTERS_TO_PROTOCOL), true);
            applySecurityToProtocol = isTrue((String) endpointProps.get(CxfConstants.APPLY_SECURITY_TO_PROTOCOL), true);
            applyTransformersToProtocol = isTrue((String) endpointProps.get(CxfConstants.APPLY_TRANSFORMERS_TO_PROTOCOL), true);
            enableHeaders = isTrue((String) endpointProps.get(CxfConstants.ENABLE_MULE_SOAP_HEADERS), true);
            
            Class<?> svcCls = null;
            Class<?> targetCls;
            
            proxy = BooleanUtils.toBoolean(proxyStr);
            
            if (proxy)
            {
                svcCls = ProxyService.class;
                targetCls = svcCls;
                frontend = "simple";
            }
            else 
            {
                if (StringUtils.isEmpty(frontend))
                {
                    frontend = connector.getDefaultFrontend();
                }
                
                if (!StringUtils.isEmpty(serviceClassName)) 
                {
                    svcCls = ClassUtils.loadClass(serviceClassName, getClass());
                } 
                
                targetCls = getTargetClass(svcCls);
                
                if (svcCls == null)
                {
                    svcCls = targetCls;
                }
            }

            ServerFactoryBean sfb;
            if (CxfConstants.SIMPLE_FRONTEND.equals(frontend))
            {
                sfb = new ServerFactoryBean();
                sfb.setDataBinding(new AegisDatabinding());
            }
            else if (CxfConstants.JAX_WS_FRONTEND.equals(frontend))
            {
                sfb = new JaxWsServerFactoryBean();
            }
            else
            {
                throw new CreateException(CxfMessages.invalidFrontend(frontend), this);
            }

            if (!proxy)
            {
                if (databinding != null && databinding.size() > 0)
                {
                    // TODO: find a way to make this not a list
                    sfb.setDataBinding(databinding.get(0));
                }
                
                if (service.getComponent() instanceof JavaComponent)
                {
                    sfb.setServiceBean(((JavaComponent) service.getComponent()).getObjectFactory().getInstance());
                }
            }
            else
            {
                sfb.setDataBinding(new StaxDataBinding());
                sfb.getFeatures().add(new StaxDataBindingFeature());
                sfb.setServiceFactory(new ProxyServiceFactoryBean());
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
                Map<String, Object> properties = sfb.getProperties();
                if (properties == null)
                {
                    properties = new HashMap<String, Object>();
                    sfb.setProperties(properties);
                }
                properties.put("mtom-enabled", mtomEnabled);
                properties.put(AttachmentOutInterceptor.WRITE_ATTACHMENTS, true);
            }
            
            sfb.setInInterceptors((List<Interceptor>) endpointProps.get("inInterceptors"));
            sfb.setInFaultInterceptors((List<Interceptor>) endpointProps.get("inFaultInterceptors"));
            sfb.setOutInterceptors((List<Interceptor>) endpointProps.get("outInterceptors"));
            sfb.setOutFaultInterceptors((List<Interceptor>) endpointProps.get("outFaultInterceptors"));

            if (sfb.getInInterceptors() == null)
            {
                sfb.setInInterceptors(new ArrayList<Interceptor>());
            }
            
            sfb.getInInterceptors().add(new MuleHeadersInInterceptor());
            
            if (sfb.getOutInterceptors() == null)
            {
                sfb.setOutInterceptors(new ArrayList<Interceptor>());
            }
            
            if (sfb.getOutFaultInterceptors() == null)
            {
                sfb.setOutFaultInterceptors(new ArrayList<Interceptor>());
            }
            
            if (enableHeaders)
            {
                sfb.getOutInterceptors().add(new MuleProtocolHeadersOutInterceptor());
                sfb.getOutFaultInterceptors().add(new MuleProtocolHeadersOutInterceptor());
            }
            
            if (proxy)
            {
                sfb.getOutInterceptors().add(new OutputPayloadInterceptor());
                sfb.getInInterceptors().add(new CopyAttachmentInInterceptor());
                sfb.getOutInterceptors().add(new CopyAttachmentOutInterceptor());
                
                if (isProxyEnvelope()) 
                {
                    sfb.getInInterceptors().add(new ReversibleStaxInInterceptor());
                    sfb.getInInterceptors().add(new ResetStaxInterceptor());
                }
            }
            
            sfb.setServiceClass(svcCls);
            sfb.setAddress(getAddressWithoutQuery());

            if (wsdlUrl != null)
            {
                sfb.setWsdlURL(wsdlUrl);
            }

            ReflectionServiceFactoryBean svcFac = sfb.getServiceFactory();

            addIgnoredMethods(svcFac, Callable.class.getName());
            addIgnoredMethods(svcFac, Initialisable.class.getName());
            addIgnoredMethods(svcFac, Disposable.class.getName());
            addIgnoredMethods(svcFac, ServiceAware.class.getName());

            String name = (String) endpointProps.get(CxfConstants.SERVICE_NAME);
            // check if there is the namespace property on the service
            String namespace = (String) endpointProps.get(CxfConstants.NAMESPACE);

            // HACK because CXF expects a QName for the service
            initServiceName(svcCls, name, namespace, svcFac);

            boolean sync = endpoint.isSynchronous();
            // default to synchronous if using http
            if (endpoint.getEndpointURI().getScheme().startsWith("http")
                || endpoint.getEndpointURI().getScheme().startsWith("servlet"))
            {
                sync = true;
            }

            sfb.setInvoker(new MuleInvoker(this, targetCls, sync));
            sfb.setStart(false);

            Bus bus = connector.getCxfBus();
            sfb.setBus(bus);

            initializeServerFactory(sfb);
            
            Configurer configurer = bus.getExtension(Configurer.class);
            if (null != configurer)
            {
                configurer.configureBean(sfb.getServiceFactory().getEndpointName().toString(), sfb);
            }

            server = sfb.create();
            
            if (proxy && isProxyEnvelope()) 
            {
                CxfUtils.removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), SoapOutInterceptor.class.getName());
            }
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
        catch (ClassNotFoundException e)
        {
            // will be thrown in the case that the ClassUtils.loadClass() does
            // not find the class to load
            throw new InitialisationException(e, this);
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    private boolean isTrue(String string, boolean defaultValue)
    {
        if (string == null) return defaultValue;
        
        return BooleanUtils.toBoolean(string);
    }

    /**
     * If any custom initialization logic needs to be done, it can
     * be done by overriding this method.
     * @param sfb
     */
    protected void initializeServerFactory(ServerFactoryBean sfb)
    {
    }

    private String getAddressWithoutQuery()
    {
        String a = getEndpointURI().getAddress();
        int idx = a.lastIndexOf('?');
        if (idx > -1) {
            a = a.substring(0, idx);
        }
        return a;
    }

    /**
     * Gross hack to support getting the service namespace from CXF if one wasn't
     * supplied.
     */
    private void initServiceName(Class<?> exposedInterface,
                                 String name,
                                 String namespace,
                                 ReflectionServiceFactoryBean svcFac)
    {
        svcFac.setServiceClass(exposedInterface);
        for (AbstractServiceConfiguration c : svcFac.getServiceConfigurations())
        {
            c.setServiceFactory(svcFac);
        }

        if (name != null && namespace == null)
        {
            namespace = svcFac.getServiceQName().getNamespaceURI();
        }
        else if (name == null && namespace != null)
        {
            name = svcFac.getServiceQName().getLocalPart();
        }

        if (name != null)
        {
            svcFac.setServiceName(new QName(namespace, name));
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

    private Class<?> getTargetClass(Class<?> svcCls) throws MuleException, ClassNotFoundException
    {
        Component component = service.getComponent();
        if (!(component instanceof JavaComponent)) 
        {
            if (svcCls == null)
            {
                throw new InitialisationException(CxfMessages.serviceClassRequiredWithPassThrough(), this);
            }
            else
            {
                return svcCls;
            }
        }
        
        try
        {
            return ((JavaComponent) component).getObjectType();
        }
        catch (Exception e)
        {
            throw new CreateException(e, this);
        }
    }

    public void doConnect() throws Exception
    {
        super.doConnect();
        
        // Start the CXF Server
        server.start();
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    public void doDisconnect() throws Exception
    {
        super.doDisconnect();
        
        server.stop();
    }

    public Server getServer()
    {
        return server;
    }

    public boolean isProxy()
    {
        return proxy;
    }

    public boolean isApplySecurityToProtocol()
    {
        return applySecurityToProtocol;
    }

    public boolean isApplyTransformersToProtocol()
    {
        return applyTransformersToProtocol;
    }

    public boolean isApplyFiltersToProtocol()
    {
        return applyFiltersToProtocol;
    }

    public boolean isProxyEnvelope()
    {
        return CxfConstants.PAYLOAD_ENVELOPE.equals(payload);
    }
}

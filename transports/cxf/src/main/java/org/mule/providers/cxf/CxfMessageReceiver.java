/*
 * $Id: XFireMessageReceiver.java 6106 2007-04-20 13:28:24Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.cxf.i18n.CxfMessages;
import org.mule.providers.cxf.support.ProviderService;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.CreateException;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
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
    private boolean bridge;

    public CxfMessageReceiver(UMOConnector umoConnector, UMOComponent component, UMOEndpoint umoEndpoint)
        throws CreateException
    {
        super(umoConnector, component, umoEndpoint);
        connector = (CxfConnector) umoConnector;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doInitialise() throws InitialisationException
    {
        try
        {
            Class<?> exposedInterface = getInterface();

            Map endpointProps = getEndpoint().getProperties();
            String wsdlUrl = (String) endpointProps.get(CxfConstants.WSDL_LOCATION);
            String databinding = (String) endpointProps.get(CxfConstants.DATA_BINDING);
            String bindingId = (String) endpointProps.get(CxfConstants.BINDING_ID);
            String frontend = (String) endpointProps.get(CxfConstants.FRONTEND);
            String bridge = (String) endpointProps.get(CxfConstants.BRIDGE);

            if (BooleanUtils.toBoolean(bridge))
            {
                exposedInterface = ProviderService.class;
                frontend = "jaxws";
            }

            if (StringUtils.isEmpty(frontend))
            {
                frontend = connector.getDefaultFrontend();
            }

            ServerFactoryBean sfb = null;
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

            // The binding - i.e. SOAP, XML, HTTP Binding, etc
            if (bindingId != null)
            {
                sfb.setBindingId(bindingId);
            }

            // Aegis, JAXB, other?
            if (databinding != null)
            {
                Class<?> c = ClassLoaderUtils.loadClass(databinding, getClass());
                sfb.setDataBinding((DataBinding) c.newInstance());
            }

            sfb.setServiceClass(exposedInterface);
            sfb.setAddress(getEndpointURI().getAddress());

            if (wsdlUrl != null)
            {
                sfb.setWsdlURL(wsdlUrl);
            }

            ReflectionServiceFactoryBean svcFac = sfb.getServiceFactory();

            addIgnoredMethods(svcFac, Callable.class.getName());
            addIgnoredMethods(svcFac, Initialisable.class.getName());
            addIgnoredMethods(svcFac, Disposable.class.getName());

            String name = component.getName();
            // check if there is the namespace property on the component
            String namespace = (String) endpointProps.get(CxfConstants.NAMESPACE);

            // HACK because CXF expects a QName for the service
            initServiceName(exposedInterface, name, namespace, svcFac);

            boolean sync = endpoint.isSynchronous();
            // default to synchronous if using http
            if (endpoint.getEndpointURI().getScheme().startsWith("http")
                || endpoint.getEndpointURI().getScheme().startsWith("servlet"))
            {
                sync = true;
            }

            sfb.setInvoker(new MuleInvoker(this, sync));
            sfb.setStart(false);

            Bus bus = connector.getCxfBus();
            sfb.setBus(bus);

            Configurer configurer = bus.getExtension(Configurer.class);
            if (null != configurer)
            {
                configurer.configureBean(sfb.getServiceFactory().getEndpointName().toString(), sfb);
            }

            server = sfb.create();
        }
        catch (UMOException e)
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
            Class c = ClassUtils.loadClass(className, getClass());
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

    private Class<?> getInterface() throws UMOException, ClassNotFoundException
    {
        Class<?> exposedInterface;
        List serviceInterfaces = (List) component.getProperties().get("serviceInterfaces");

        if (serviceInterfaces == null)
        {
            try
            {
                exposedInterface = component.getServiceFactory().getOrCreate().getClass();
            }
            catch (Exception e)
            {
                throw new CreateException(e, this);
            }
        }

        else
        {
            String className = (String) serviceInterfaces.get(0);
            exposedInterface = ClassUtils.loadClass(className, this.getClass());
            logger.info(className + " class was used to expose your service");

            if (serviceInterfaces.size() > 1)
            {
                logger.info("Only the first class was used to expose your method");
            }
        }
        return exposedInterface;
    }

    protected void doDispose()
    {
        // template method
    }

    public void doConnect() throws Exception
    {
        // Start the CXF Server
        server.start();
        connector.registerReceiverWithMuleService(this, endpoint.getEndpointURI());
    }

    public void doDisconnect() throws Exception
    {
        server.stop();
    }

    public void doStart() throws UMOException
    {
        // nothing to do
    }

    public void doStop() throws UMOException
    {
        // nothing to do
    }

    public Server getServer()
    {
        return server;
    }

    public boolean isBridge()
    {
        return bridge;
    }

}

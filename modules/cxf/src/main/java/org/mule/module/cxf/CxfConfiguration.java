/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.spring.SpringRegistry;
import org.mule.module.cxf.support.MuleHeadersInInterceptor;
import org.mule.module.cxf.support.MuleHeadersOutInterceptor;
import org.mule.module.cxf.support.MuleProtocolHeadersOutInterceptor;
import org.mule.module.cxf.transport.MuleUniversalTransport;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.springframework.context.ApplicationContext;

/**
 * Provides global CXF configuration defaults.
 */
public class CxfConfiguration implements Initialisable, Disposable, MuleContextAware
{
    public static final String CXF = "cxf";
    public static final String CONFIGURATION_LOCATION = "configurationLocation";
    public static final String DEFAULT_MULE_NAMESPACE_URI = "http://www.muleumo.org";
    public static final String BUS_PROPERTY = CXF;

    protected transient Log logger = LogFactory.getLog(getClass());

    // The CXF Bus object
    private Bus bus;
    private String configurationLocation;
    private boolean initializeStaticBusInstance;
    private MuleContext muleContext;
    private boolean enableMuleSoapHeaders = true;

    public void initialise() throws InitialisationException
    {
        BusFactory.setDefaultBus(null);
        ApplicationContext context = (ApplicationContext) muleContext.getRegistry().lookupObject(SpringRegistry.SPRING_APPLICATION_CONTEXT);

        if (configurationLocation != null)
        {
            bus = new SpringBusFactory(context).createBus(configurationLocation, true);
        }
        else
        {
            bus = new SpringBusFactory().createBus((String) null, true);

        }

        if (!initializeStaticBusInstance)
        {
            BusFactory.setDefaultBus(null);
        }

        MuleUniversalTransport transport = new MuleUniversalTransport(this);
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/soap/http", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http/configuration", transport);
        dfm.registerDestinationFactory("http://schemas.xmlsoap.org/wsdl/http/", transport);
        dfm.registerDestinationFactory("http://www.w3.org/2003/05/soap/bindings/HTTP/", transport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/jms", transport);
        dfm.registerDestinationFactory("http://cxf.apache.org/transports/http", transport);

        dfm.registerDestinationFactory(MuleUniversalTransport.TRANSPORT_ID, transport);

        ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);
        try
        {
            // Force the HTTP transport to load if available, otherwise it could
            // overwrite our namespaces later
            extension.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
        }
        catch (BusException e1)
        {
            // If unavailable eat exception and continue
        }
        extension.registerConduitInitiator("http://cxf.apache.org/transports/http", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http/", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/http", transport);
        extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/http/", transport);
        extension.registerConduitInitiator("http://www.w3.org/2003/05/soap/bindings/HTTP/", transport);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/http/configuration", transport);
        extension.registerConduitInitiator("http://cxf.apache.org/bindings/xformat", transport);
        extension.registerConduitInitiator("http://cxf.apache.org/transports/jms", transport);
        extension.registerConduitInitiator(MuleUniversalTransport.TRANSPORT_ID, transport);

        bus.getOutInterceptors().add(new MuleProtocolHeadersOutInterceptor());
        bus.getOutFaultInterceptors().add(new MuleProtocolHeadersOutInterceptor());

        if (enableMuleSoapHeaders)
        {
            bus.getInInterceptors().add(new MuleHeadersInInterceptor());
            bus.getInFaultInterceptors().add(new MuleHeadersInInterceptor());
            bus.getOutInterceptors().add(new MuleHeadersOutInterceptor());
            bus.getOutFaultInterceptors().add(new MuleHeadersOutInterceptor());
        }
    }

    public void dispose()
    {
        bus.shutdown(true);
        // clear static caches preventing memory leaks on redeploy
        // needed because cxf core classes are loaded by the Mule system classloader, not app's
        JAXBContextCache.clearCaches();
        try
        {
            // ASMHelper.LOADER_MAP is a protected static field
            final Field cacheField = ASMHelper.class.getDeclaredField("LOADER_MAP");
            cacheField.setAccessible(true);
            // static field
            final Map cache = (Map) cacheField.get(null);
            cache.clear();
        }
        catch (Throwable t)
        {
            logger.warn("Error disposing CxfConfiguration, this may cause a memory leak", t);
        }
    }

    public Bus getCxfBus()
    {
        return bus;
    }

    public void setCxfBus(Bus bus)
    {
        this.bus = bus;
    }

    public String getConfigurationLocation()
    {
        return configurationLocation;
    }

    public void setConfigurationLocation(String configurationLocation)
    {
        this.configurationLocation = configurationLocation;
    }
    
    public boolean isInitializeStaticBusInstance()
    {
        return initializeStaticBusInstance;
    }

    public void setInitializeStaticBusInstance(boolean initializeStaticBusInstance)
    {
        this.initializeStaticBusInstance = initializeStaticBusInstance;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public static CxfConfiguration getConfiguration(MuleContext muleContext) throws MuleException
    {
        CxfConfiguration configuration = muleContext.getRegistry().get(CxfConstants.DEFAULT_CXF_CONFIGURATION);
        if (configuration == null)
        {
            configuration = new CxfConfiguration();
            configuration.setMuleContext(muleContext);
            configuration.initialise();
            muleContext.getRegistry().registerObject(CxfConstants.DEFAULT_CXF_CONFIGURATION, configuration);
        }
        return configuration;
    }

    public boolean isEnableMuleSoapHeaders()
    {
        return enableMuleSoapHeaders;
    }

    public void setEnableMuleSoapHeaders(boolean enableMuleSoapHeaders)
    {
        this.enableMuleSoapHeaders = enableMuleSoapHeaders;
    }

}

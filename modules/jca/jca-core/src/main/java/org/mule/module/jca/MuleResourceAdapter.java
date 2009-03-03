/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.jca;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleResourceAdapter</code> TODO
 */
public class MuleResourceAdapter implements ResourceAdapter, Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5727648958127416509L;

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(this.getClass());

    protected transient MuleContext muleContext;

    protected transient BootstrapContext bootstrapContext;
    protected MuleConnectionRequestInfo info = new MuleConnectionRequestInfo();
    protected final Map endpoints = new HashMap();
    protected String defaultJcaModelName;

    public MuleResourceAdapter()
    {
        // TODO Make this work for OSGi
        //RegistryContext.getOrCreateRegistry();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
        this.logger = LogFactory.getLog(this.getClass());
    }

    /**
     * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
     */
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException
    {
        this.bootstrapContext = bootstrapContext;

        if (info.getConfigurations() != null)
        {
            ConfigurationBuilder configBuilder = null;
            try
            {
                configBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(info.getConfigurationBuilder(),
                                                                                   info.getConfigurations());
            }
            catch (Exception e)
            {
                throw new ResourceAdapterInternalException("Failed to instanciate configurationBuilder class: "
                                                           + info.getConfigurationBuilder(), e);
            }

            try
            {
                logger.info("Initializing Mule...");

                MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
                DefaultMuleConfiguration config = new DefaultMuleConfiguration();
                config.setSystemModelType(JcaModel.JCA_MODEL_TYPE);
                contextBuilder.setMuleConfiguration(config);
                muleContext = new DefaultMuleContextFactory().createMuleContext(configBuilder, contextBuilder);
            }
            catch (MuleException e)
            {
                logger.error(e);
                throw new ResourceAdapterInternalException(
                    "Failed to load configurations: " + info.getConfigurations(), e);
            }
            try
            {
                logger.info("Starting Mule...");
                muleContext.start();
            }
            catch (MuleException e)
            {
                logger.error(e);
                throw new ResourceAdapterInternalException("Failed to start management context", e);
            }
        }
    }

    /**
     * @see javax.resource.spi.ResourceAdapter#stop()
     */
    public void stop()
    {
        logger.info("Stopping Mule...");
        muleContext.dispose();
        muleContext = null;
        bootstrapContext = null;
    }

    /**
     * @return the bootstrap context for this adapter
     */
    public BootstrapContext getBootstrapContext()
    {
        return bootstrapContext;
    }

    /**
     * @see javax.resource.spi.ResourceAdapter#endpointActivation(javax.resource.spi.endpoint.MessageEndpointFactory,
     *      javax.resource.spi.ActivationSpec)
     */
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec)
        throws ResourceException
    {
        if (activationSpec.getResourceAdapter() != this)
        {
            throw new ResourceException("ActivationSpec not initialized with this ResourceAdapter instance");
        }

        if (activationSpec.getClass().equals(MuleActivationSpec.class))
        {
            MuleActivationSpec muleActivationSpec = (MuleActivationSpec) activationSpec;
            try
            {
                // Resolve modelName
                String modelName = resolveModelName(muleActivationSpec);

                // Lookup/create JCA Model
                JcaModel model = getJcaModel(modelName);

                // Create Endpoint
                InboundEndpoint endpoint = createMessageInflowEndpoint(muleActivationSpec);

                // Create Service
                Service service = createJcaService(endpointFactory, model, endpoint);

                // Keep reference to JcaService descriptor for endpointDeactivation
                MuleEndpointKey key = new MuleEndpointKey(endpointFactory, muleActivationSpec);
                endpoints.put(key, service);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        else
        {
            throw new NotSupportedException("That type of ActicationSpec not supported: " + activationSpec.getClass());
        }

    }

    /**
     * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory,
     *      javax.resource.spi.ActivationSpec)
     */
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec)
    {
        if (activationSpec.getClass().equals(MuleActivationSpec.class))
        {
            MuleActivationSpec muleActivationSpec = (MuleActivationSpec) activationSpec;
            MuleEndpointKey key = new MuleEndpointKey(endpointFactory, (MuleActivationSpec) activationSpec);
            Service service = (Service) endpoints.remove(key);
            if (service == null)
            {
                logger.warn("No endpoint was registered with key: " + key);
                return;
            }

            // Resolve modelName
            String modelName = null;
            try
            {
                modelName = resolveModelName(muleActivationSpec);
            }
            catch (ResourceException e)
            {
                logger.error(e.getMessage(), e);
            }

            try
            {
                muleContext.getRegistry().unregisterService(service.getName());
            }
            catch (MuleException e)
            {
                logger.error(e.getMessage(), e);
            }
        }
    }

    protected String resolveModelName(MuleActivationSpec activationSpec) throws ResourceException
    {
        // JCA specification mentions activationSpec properties inheriting
        // resourceAdaptor properties, but this doesn't seem to work, at
        // least with JBOSS, so do it manually.
        String modelName = activationSpec.getModelName();
        if (modelName == null)
        {
            modelName = defaultJcaModelName;
        }
        if (modelName == null)
        {
            throw new ResourceException(
                "The 'modelName' property has not been configured for either the MuleResourceAdaptor or MuleActicationSpec.");
        }
        return modelName;
    }

    protected JcaModel getJcaModel(String modelName) throws MuleException, ResourceException
    {
        Model model = muleContext.getRegistry().lookupModel(modelName);
        if (model != null)
        {
            if (model instanceof JcaModel)
            {
                return (JcaModel) model;
            }
            else
            {
                throw new ResourceException("Model:-" + modelName + "  is not compatible with JCA type");
            }
        }
        else
        {
            JcaModel jcaModel = new JcaModel();
            jcaModel.setName(modelName);
            muleContext.getRegistry().registerModel(jcaModel);
            return jcaModel;
        }
    }

    protected Service createJcaService(MessageEndpointFactory endpointFactory,
                                              JcaModel model,
                                              InboundEndpoint endpoint) throws MuleException
    {
        String name = "JcaService#" + endpointFactory.hashCode();
        Service service = new JcaService();
        service.setName(name);
        service.getInboundRouter().addEndpoint(endpoint);

        // Set endpointFactory rather than endpoint here, so we can obtain a
        // new endpoint instance from factory for each incoming message in
        // JcaComponet as reccomended by JCA specification
        service.setComponent(new JcaComponent(endpointFactory, model.getEntryPointResolverSet(), service,
            new DelegateWorkManager(bootstrapContext.getWorkManager())));
        service.setModel(model);
        muleContext.getRegistry().registerService(service);
        return service;
    }

    protected InboundEndpoint createMessageInflowEndpoint(MuleActivationSpec muleActivationSpec)
        throws MuleException
    {
        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder(
            muleActivationSpec.getEndpoint()), muleContext);

        // Use asynchronous endpoint as we need to dispatch to service
        // rather than send.
        endpointBuilder.setSynchronous(false);

        return muleContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(endpointBuilder);
    }

    /**
     * We only connect to one resource manager per ResourceAdapter instance, so any ActivationSpec will return
     * the same XAResource.
     * 
     * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
     */
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException
    {
        return new XAResource[]{};
    }

    /**
     * @return
     */
    public String getPassword()
    {
        return info.getPassword();
    }

    /**
     * @return
     */
    public String getConfigurations()
    {
        return info.getConfigurations();
    }

    /**
     * @return
     */
    public String getUserName()
    {
        return info.getUserName();
    }

    /**
     * @param password
     */
    public void setPassword(String password)
    {
        info.setPassword(password);
    }

    /**
     * @param configurations
     */
    public void setConfigurations(String configurations)
    {
        info.setConfigurations(configurations);
    }

    /**
     * @param userid
     */
    public void setUserName(String userid)
    {
        info.setUserName(userid);
    }

    public String getConfigurationBuilder()
    {
        return info.getConfigurationBuilder();
    }

    public void setConfigurationBuilder(String configbuilder)
    {
        info.setConfigurationBuilder(configbuilder);
    }

    /**
     * @return Returns the info.
     */
    public MuleConnectionRequestInfo getInfo()
    {
        return info;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof MuleResourceAdapter))
        {
            return false;
        }

        final MuleResourceAdapter muleResourceAdapter = (MuleResourceAdapter) o;

        if (info != null ? !info.equals(muleResourceAdapter.info) : muleResourceAdapter.info != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return (info != null ? info.hashCode() : 0);
    }

    public String getModelName()
    {
        return defaultJcaModelName;
    }

    public void setModelName(String modelName)
    {
        this.defaultJcaModelName = modelName;
    }

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

import org.mule.RegistryContext;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.ThreadingProfile;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.ClassUtils;
import org.mule.util.object.SingletonObjectFactory;

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
import javax.resource.spi.endpoint.MessageEndpoint;
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

    private transient BootstrapContext bootstrapContext;
    private MuleConnectionRequestInfo info = new MuleConnectionRequestInfo();
    private final Map endpoints = new HashMap();
    private UMOManagementContext managementContext;

    public MuleResourceAdapter()
    {
        RegistryContext.getConfiguration().setSystemModelType(JcaModel.JCA_MODEL_TYPE);
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

                ConfigurationBuilder builder = null;
                try
                {
                    builder = (ConfigurationBuilder)ClassUtils.instanciateClass(
                        info.getConfigurationBuilder(), ClassUtils.NO_ARGS);

                }
                catch (Exception e)
                {
                    throw new ResourceAdapterInternalException(
                        "Failed to instanciate configurationBuilder class: " + info.getConfigurationBuilder(),
                        e);
                }

                try
                {
                    managementContext = builder.configure(info.getConfigurations(), null);
                }
                catch (ConfigurationException e)
                {
                    logger.error(e);
                    throw new ResourceAdapterInternalException("Failed to load configurations: "
                                                               + info.getConfigurations(), e);
                }
        }
    }

    /**
     * @see javax.resource.spi.ResourceAdapter#stop()
     */
    public void stop()
    {
        managementContext.dispose();
        managementContext = null;
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

            try
            {
                UMOEndpointURI uri = new MuleEndpointURI(((MuleActivationSpec) activationSpec).getEndpoint());
                UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getEndpoint(
                    uri, UMOEndpoint.ENDPOINT_TYPE_RECEIVER, managementContext);

                ((AbstractConnector)endpoint.getConnector()).getReceiverThreadingProfile()
                    .setWorkManagerFactory(new ThreadingProfile.WorkManagerFactory()
                    {
                        public UMOWorkManager createWorkManager(ThreadingProfile profile, String name)
                        {
                            return new DelegateWorkManager(bootstrapContext.getWorkManager());

                        }
                    });
                // TODO manage transactions
                MessageEndpoint messageEndpoint = null;
                messageEndpoint = endpointFactory.createEndpoint(null);

                String name = "JcaComponent#" + messageEndpoint.hashCode();
                UMOComponent component = new SedaComponent();
                component.setName(name);
                component.getInboundRouter().addEndpoint(endpoint);
                component.setServiceFactory(new SingletonObjectFactory(messageEndpoint));
                managementContext.getRegistry().registerComponent(component, managementContext);

                MuleEndpointKey key = new MuleEndpointKey(endpointFactory, (MuleActivationSpec)activationSpec);

                endpoints.put(key, component);
            }
            catch (Exception e)
            {
                logger.error(e.getMessage(), e);
            }
        }
        else
        {
            throw new NotSupportedException("That type of ActicationSpec not supported: "
                                            + activationSpec.getClass());
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
            MuleEndpointKey key = new MuleEndpointKey(endpointFactory, (MuleActivationSpec)activationSpec);
            UMOComponent component = (UMOComponent)endpoints.get(key);
            if (component == null)
            {
                logger.warn("No endpoint was registered with key: " + key);
                return;
            }
            managementContext.getRegistry().unregisterComponent(component.getName());
        }

    }

    /**
     * We only connect to one resource manager per ResourceAdapter instance, so any
     * ActivationSpec will return the same XAResource.
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

        final MuleResourceAdapter muleResourceAdapter = (MuleResourceAdapter)o;

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

}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jca;

import org.mule.DefaultMuleContext;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.builders.DeployableMuleXmlContextListener;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
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
    protected final Map<MuleEndpointKey, Service> endpoints = new HashMap<MuleEndpointKey, Service>();
    protected String defaultJcaModelName;

    private String configurationBuilder = SpringXmlConfigurationBuilder.class.getName();
    private String configurations;
    private String username;
    private String password;

    private DefaultMuleConfiguration muleConfiguration = new DefaultMuleConfiguration();

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException
    {
        ois.defaultReadObject();
        this.logger = LogFactory.getLog(this.getClass());
    }

    /**
     * @see javax.resource.spi.ResourceAdapter#start(javax.resource.spi.BootstrapContext)
     */
    @Override
    public void start(BootstrapContext bootstrapContext) throws ResourceAdapterInternalException
    {
        this.bootstrapContext = bootstrapContext;

        if (configurations != null)
        {
            ConfigurationBuilder configBuilder = null;
            try
            {
                configBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(configurationBuilder,
                    configurations);
            }
            catch (Exception e)
            {
                throw new ResourceAdapterInternalException(
                    "Failed to instanciate configurationBuilder class: " + configurationBuilder, e);
            }

            try
            {
                logger.info("Initializing Mule...");

                MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
                muleConfiguration.setSystemModelType(JcaModel.JCA_MODEL_TYPE);
                contextBuilder.setMuleConfiguration(muleConfiguration);
                muleContext = new DefaultMuleContextFactory().createMuleContext(configBuilder, contextBuilder);

                // Make single shared application server instance of mule context
                // available to DeployableMuleXmlContextListener to support hot
                // deployment of Mule configurations in web applications.
                DeployableMuleXmlContextListener.setMuleContext(muleContext);
            }
            catch (MuleException e)
            {
                logger.error(e);
                throw new ResourceAdapterInternalException(
                    "Failed to load configurations: " + configurations, e);
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
    @Override
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
    @Override
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
            throw new NotSupportedException("That type of ActicationSpec not supported: "
                                            + activationSpec.getClass());
        }

    }

    /**
     * @see javax.resource.spi.ResourceAdapter#endpointDeactivation(javax.resource.spi.endpoint.MessageEndpointFactory,
     *      javax.resource.spi.ActivationSpec)
     */
    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec activationSpec)
    {
        if (activationSpec.getClass().equals(MuleActivationSpec.class))
        {
            MuleActivationSpec muleActivationSpec = (MuleActivationSpec) activationSpec;
            MuleEndpointKey key = new MuleEndpointKey(endpointFactory, (MuleActivationSpec) activationSpec);
            Service service = endpoints.remove(key);
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
        Service service = new JcaService(muleContext);
        service.setName(name);
        ((CompositeMessageSource) service.getMessageSource()).addSource(endpoint);

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
            muleActivationSpec.getEndpoint(), muleContext));

        endpointBuilder.setExchangePattern(MessageExchangePattern.ONE_WAY);

        return muleContext.getEndpointFactory().getInboundEndpoint(endpointBuilder);
    }

    /**
     * We only connect to one resource manager per ResourceAdapter instance, so any
     * ActivationSpec will return the same XAResource.
     *
     * @see javax.resource.spi.ResourceAdapter#getXAResources(javax.resource.spi.ActivationSpec[])
     */
    @Override
    public XAResource[] getXAResources(ActivationSpec[] activationSpecs) throws ResourceException
    {
        return new XAResource[]{};
    }

    /**
     * @param password
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * @param configurations
     */
    public void setConfigurations(String configurations)
    {
        this.configurations = configurations;
    }

    /**
     * @param userid
     */
    public void setUserName(String userid)
    {
        this.username = userid;
    }

    public void setConfigurationBuilder(String configbuilder)
    {
        this.configurationBuilder = configbuilder;
    }

    public void setModelName(String modelName)
    {
        this.defaultJcaModelName = modelName;
    }

    public void setAutoWrapMessageAwareTransform(Boolean autoWrapMessageAwareTransform)
    {
        if (autoWrapMessageAwareTransform != null)
        {
            muleConfiguration.setAutoWrapMessageAwareTransform(autoWrapMessageAwareTransform);
        }
    }

    public void setCacheMessageAsBytes(Boolean cacheMessageAsBytes)
    {
        if (cacheMessageAsBytes != null)
        {
            muleConfiguration.setCacheMessageAsBytes(cacheMessageAsBytes);
        }
    }

    public void setCacheMessageOriginalPayload(Boolean cacheMessageOriginalPayload)
    {
        if (cacheMessageOriginalPayload != null)
        {
            muleConfiguration.setCacheMessageOriginalPayload(cacheMessageOriginalPayload);
        }
    }

    public void setClusterId(String clusterId)
    {
        ((DefaultMuleContext) muleContext).setClusterId(clusterId);
    }

    public void setDefaultEncoding(String encoding)
    {
        muleConfiguration.setDefaultEncoding(encoding);
    }

    public void setDefaultQueueTimeout(Integer defaultQueueTimeout)
    {
        if (defaultQueueTimeout != null)
        {
            muleConfiguration.setDefaultQueueTimeout(defaultQueueTimeout);
        }
    }

    public void setDefaultResponseTimeout(Integer responseTimeout)
    {
        if (responseTimeout != null)
        {
            muleConfiguration.setDefaultResponseTimeout(responseTimeout);
        }
    }

    public void setDefaultSynchronousEndpoints(Boolean synchronous)
    {
        if (synchronous != null)
        {
            muleConfiguration.setDefaultSynchronousEndpoints(synchronous);
        }
    }

    public void setDefaultTransactionTimeout(Integer defaultTransactionTimeout)
    {
        if (defaultTransactionTimeout != null)
        {
            muleConfiguration.setDefaultTransactionTimeout(defaultTransactionTimeout);
        }
    }

    public void setDomainId(String domainId)
    {
        muleConfiguration.setDomainId(domainId);
    }

    public void setServerId(String serverId)
    {
        muleConfiguration.setId(serverId);
    }

    public void setShutdownTimeout(Integer shutdownTimeout)
    {
        if (shutdownTimeout != null)
        {
            muleConfiguration.setShutdownTimeout(shutdownTimeout);
        }
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        muleConfiguration.setWorkingDirectory(workingDirectory);
    }

    // Although get methods for config properties aren't really required we need to
    // include them otherwise Geronimo does not consider them valid properties

    public String getConfigurationBuilder()
    {
        return configurationBuilder;
    }

    public String getConfigurations()
    {
        return configurations;
    }

    public String getUserName()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public String getModelName()
    {
        return defaultJcaModelName;
    }

    public String getClusterId()
    {
        return muleContext.getClusterId();
    }

    public String getDefaultEncoding()
    {
        return muleConfiguration.getDefaultEncoding();
    }

    public int getDefaultQueueTimeout()
    {
        return muleConfiguration.getDefaultQueueTimeout();
    }

    public int getDefaultResponseTimeout()
    {
        return muleConfiguration.getDefaultResponseTimeout();
    }

    public int getDefaultTransactionTimeout()
    {
        return muleConfiguration.getDefaultTransactionTimeout();
    }

    public String getDomainId()
    {
        return muleConfiguration.getDomainId();
    }

    public String getId()
    {
        return muleConfiguration.getId();
    }

    public String getMuleHomeDirectory()
    {
        return muleConfiguration.getMuleHomeDirectory();
    }

    public int getShutdownTimeout()
    {
        return muleConfiguration.getShutdownTimeout();
    }

    public String getSystemModelType()
    {
        return muleConfiguration.getSystemModelType();
    }

    public String getSystemName()
    {
        return muleConfiguration.getSystemName();
    }

    public String getWorkingDirectory()
    {
        return muleConfiguration.getWorkingDirectory();
    }

    public boolean isAutoWrapMessageAwareTransform()
    {
        return muleConfiguration.isAutoWrapMessageAwareTransform();
    }

    public boolean isCacheMessageAsBytes()
    {
        return muleConfiguration.isCacheMessageAsBytes();
    }

    public boolean isCacheMessageOriginalPayload()
    {
        return muleConfiguration.isCacheMessageOriginalPayload();
    }

    public boolean isClientMode()
    {
        return muleConfiguration.isClientMode();
    }

    public boolean isEnableStreaming()
    {
        return muleConfiguration.isEnableStreaming();
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((configurationBuilder == null) ? 0 : configurationBuilder.hashCode());
        result = prime * result + ((configurations == null) ? 0 : configurations.hashCode());
        result = prime * result + ((defaultJcaModelName == null) ? 0 : defaultJcaModelName.hashCode());
        result = prime * result + ((endpoints == null) ? 0 : endpoints.hashCode());
        result = prime * result + ((muleConfiguration == null) ? 0 : muleConfiguration.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        MuleResourceAdapter other = (MuleResourceAdapter) obj;
        if (configurationBuilder == null)
        {
            if (other.configurationBuilder != null) return false;
        }
        else if (!configurationBuilder.equals(other.configurationBuilder)) return false;
        if (configurations == null)
        {
            if (other.configurations != null) return false;
        }
        else if (!configurations.equals(other.configurations)) return false;
        if (defaultJcaModelName == null)
        {
            if (other.defaultJcaModelName != null) return false;
        }
        else if (!defaultJcaModelName.equals(other.defaultJcaModelName)) return false;
        if (endpoints == null)
        {
            if (other.endpoints != null) return false;
        }
        else if (!endpoints.equals(other.endpoints)) return false;
        if (muleConfiguration == null)
        {
            if (other.muleConfiguration != null) return false;
        }
        else if (!muleConfiguration.equals(other.muleConfiguration)) return false;
        if (password == null)
        {
            if (other.password != null) return false;
        }
        else if (!password.equals(other.password)) return false;
        if (username == null)
        {
            if (other.username != null) return false;
        }
        else if (!username.equals(other.username)) return false;
        return true;
    }

}

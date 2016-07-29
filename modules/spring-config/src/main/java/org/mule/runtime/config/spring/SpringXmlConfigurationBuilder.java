/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ParentMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.runtime.core.config.i18n.MessageFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a
 * Spring XML Configuration file used with Mule name-spaces. Multiple configuration
 * files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder implements ParentMuleContextAwareConfigurationBuilder
{

    private ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(emptyList());
    protected boolean useDefaultConfigResource = true;
    protected boolean useMinimalConfigResource = false;

    protected SpringRegistry registry;

    protected ApplicationContext domainContext;
    protected ApplicationContext parentContext;
    protected ApplicationContext applicationContext;
    private ArtifactType artifactType;
    private final List<MuleContextServiceConfigurator> serviceConfigurators = new ArrayList<>();

    public SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties, ArtifactType artifactType) throws ConfigurationException
    {
        super(configResources, artifactProperties);
        this.artifactType = artifactType;
    }

    public SpringXmlConfigurationBuilder(String configResources, Map<String, String> artifactProperties, ArtifactType artifactType) throws ConfigurationException
    {
        this(new String[]{configResources}, artifactProperties, artifactType);
    }

    public SpringXmlConfigurationBuilder(ConfigResource[] configResources, Map<String, String> artifactProperties, ArtifactType artifactType)
    {
        super(configResources, artifactProperties);
        this.artifactType = artifactType;
    }

    public SpringXmlConfigurationBuilder(String configResource) throws ConfigurationException
    {
        this(configResource, emptyMap(), APP);
    }

    public SpringXmlConfigurationBuilder(String[] configFiles) throws ConfigurationException
    {
        super(configFiles, emptyMap());
        this.artifactType = APP;
    }

    public SpringXmlConfigurationBuilder(String[] configurationFiles, ArtifactConfiguration artifactConfiguration, Map<String, String> artifactProperties, ArtifactType artifactType) throws ConfigurationException
    {
        this(configurationFiles, artifactProperties, artifactType);
        this.artifactConfiguration = artifactConfiguration;
        this.artifactType = APP;
    }

    /**
     * Adds a service configurator to be used on the context being built.
     *
     * @param serviceConfigurator service to add. Non null.
     */
    public void addServiceConfigurator(MuleContextServiceConfigurator serviceConfigurator)
    {
        checkArgument(serviceConfigurator != null, "serviceConfigurator cannot be null");
        serviceConfigurators.add(serviceConfigurator);
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        applicationContext = createApplicationContext(muleContext);
        createSpringRegistry(muleContext, applicationContext);
    }

    /**
     * Template method for modifying the list of resources to be loaded.
     * This operation is a no-op by default.
     *
     * @param allResources the list of {@link ConfigResource} to be loaded
     */
    @SuppressWarnings("unused")
    protected void addResources(List<ConfigResource> allResources)
    {
    }

    public void unconfigure(MuleContext muleContext)
    {
        registry.dispose();
        if (muleContext != null)
        {
            muleContext.removeRegistry(registry);
        }
        registry = null;
        configured = false;
    }

    private ApplicationContext createApplicationContext(MuleContext muleContext) throws Exception
    {
        OptionalObjectsController applicationObjectcontroller = new DefaultOptionalObjectsController();
        OptionalObjectsController parentObjectController = null;
        ApplicationContext parentApplicationContext = parentContext != null ? parentContext : domainContext;

        if (parentApplicationContext instanceof MuleArtifactContext)
        {
            parentObjectController = ((MuleArtifactContext) parentApplicationContext).getOptionalObjectsController();
        }

        if (parentObjectController != null)
        {
            applicationObjectcontroller = new CompositeOptionalObjectsController(applicationObjectcontroller, parentObjectController);
        }
        //TODO MULE-10084 : Refactor to only accept artifactConfiguration and not artifactConfigResources
        final ApplicationContext applicationContext = doCreateApplicationContext(muleContext, artifactConfigResources, artifactConfiguration, applicationObjectcontroller);
        serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));
        return applicationContext;
    }

    protected ApplicationContext doCreateApplicationContext(MuleContext muleContext, ConfigResource[] artifactConfigResources, ArtifactConfiguration artifactConfiguration, OptionalObjectsController optionalObjectsController)
    {
        return new MuleArtifactContext(muleContext, artifactConfigResources, artifactConfiguration, optionalObjectsController, getArtifactProperties(), artifactType);
    }


    protected void createSpringRegistry(MuleContext muleContext, ApplicationContext applicationContext)
            throws Exception
    {
        if (parentContext != null && domainContext != null)
        {
            throw new IllegalStateException("An application with a web xml context and domain resources is not supported");
        }
        if (parentContext != null)
        {
            createRegistryWithParentContext(muleContext, applicationContext, parentContext);
        }
        else if (domainContext != null)
        {
            createRegistryWithParentContext(muleContext, applicationContext, domainContext);
        }
        else
        {
            registry = new SpringRegistry(applicationContext, muleContext);
        }

        // Note: The SpringRegistry must be created before
        // applicationContext.refresh() gets called because
        // some beans may try to look up other beans via the Registry during
        // preInstantiateSingletons().
        muleContext.addRegistry(registry);
    }

    private void createRegistryWithParentContext(MuleContext muleContext, ApplicationContext applicationContext, ApplicationContext parentContext) throws ConfigurationException
    {
        if (applicationContext instanceof ConfigurableApplicationContext)
        {
            registry = new SpringRegistry((ConfigurableApplicationContext) applicationContext,
                                          parentContext, muleContext);
        }
        else
        {
            throw new ConfigurationException(
                    MessageFactory.createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
        }
    }

    @Override
    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception
    {
        // If the MuleContext is started, start all objects in the new Registry.
        if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME))
        {
            lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
        }
    }

    public void setUseDefaultConfigResource(boolean useDefaultConfigResource)
    {
        this.useDefaultConfigResource = useDefaultConfigResource;
    }

    public ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    public void setUseMinimalConfigResource(boolean useMinimalConfigResource)
    {
        this.useMinimalConfigResource = useMinimalConfigResource;
    }

    protected ApplicationContext getParentContext()
    {
        return parentContext;
    }

    public void setParentContext(ApplicationContext parentContext)
    {
        this.parentContext = parentContext;
    }

    @Override
    public void setParentContext(MuleContext domainContext)
    {
        this.domainContext = domainContext.getRegistry().get("springApplicationContext");
    }
}

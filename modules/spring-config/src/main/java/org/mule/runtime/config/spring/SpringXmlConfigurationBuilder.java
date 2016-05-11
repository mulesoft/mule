/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.runtime.core.config.i18n.MessageFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a
 * Spring XML Configuration file used with Mule name-spaces. Multiple configuration
 * files can be loaded from this builder (specified as a comma-separated list).
 */
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder implements DomainMuleContextAwareConfigurationBuilder
{

    public static final String MULE_DEFAULTS_CONFIG = "default-mule-config.xml";
    public static final String MULE_SPRING_CONFIG = "mule-spring-config.xml";
    public static final String MULE_MINIMAL_CONFIG = "minimal-mule-config.xml";
    public static final String MULE_MINIMAL_SPRING_CONFIG = "minimal-mule-config-beans.xml";
    public static final String MULE_REGISTRY_BOOTSTRAP_SPRING_CONFIG = "registry-bootstrap-mule-config.xml";
    public static final String MULE_DOMAIN_REGISTRY_BOOTSTRAP_SPRING_CONFIG = "registry-bootstrap-mule-domain-config.xml";

    /**
     * Prepend "default-mule-config.xml" to the list of config resources.
     */
    protected boolean useDefaultConfigResource = true;
    protected boolean useMinimalConfigResource = false;

    protected SpringRegistry registry;

    protected ApplicationContext domainContext;
    protected ApplicationContext parentContext;
    protected ApplicationContext applicationContext;

    public SpringXmlConfigurationBuilder(String[] configResources) throws ConfigurationException
    {
        super(configResources);
    }

    public SpringXmlConfigurationBuilder(String configResources) throws ConfigurationException
    {
        super(configResources);
    }

    public SpringXmlConfigurationBuilder(ConfigResource[] configResources)
    {
        super(configResources);
    }

    protected List<ConfigResource> getConfigResources() throws IOException
    {
        List<ConfigResource> allResources = new ArrayList<>();
        if (useMinimalConfigResource)
        {
            allResources.add(new ConfigResource(MULE_DOMAIN_REGISTRY_BOOTSTRAP_SPRING_CONFIG));
            allResources.add(new ConfigResource(MULE_MINIMAL_SPRING_CONFIG));
            allResources.add(new ConfigResource(MULE_MINIMAL_CONFIG));
            allResources.add(new ConfigResource(MULE_SPRING_CONFIG));
            allResources.addAll(Arrays.asList(configResources));
        }
        else if (useDefaultConfigResource)
        {
            allResources.add(new ConfigResource(MULE_REGISTRY_BOOTSTRAP_SPRING_CONFIG));
            allResources.add(new ConfigResource(MULE_MINIMAL_SPRING_CONFIG));
            allResources.add(new ConfigResource(MULE_MINIMAL_CONFIG));
            allResources.add(new ConfigResource(MULE_SPRING_CONFIG));
            allResources.add( new ConfigResource(MULE_DEFAULTS_CONFIG));
            allResources.addAll(Arrays.asList(configResources));
        }
        else
        {
            allResources.add(new ConfigResource(MULE_SPRING_CONFIG));
            allResources.addAll(Arrays.asList(configResources));
        }
        return allResources;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        List<ConfigResource> allResources = getConfigResources();
        addResources(allResources);
        ConfigResource[] configResourcesArray = new ConfigResource[allResources.size()];
        applicationContext = createApplicationContext(muleContext, allResources.toArray(configResourcesArray));
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

    private ApplicationContext createApplicationContext(MuleContext muleContext,
                                                        ConfigResource[] configResources) throws Exception
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

        return doCreateApplicationContext(muleContext, configResources, applicationObjectcontroller);
    }

    protected ApplicationContext doCreateApplicationContext(MuleContext muleContext, ConfigResource[] configResources, OptionalObjectsController optionalObjectsController)
    {
        return new MuleArtifactContext(muleContext, configResources, optionalObjectsController);
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

        registry.initialise();
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

    public boolean isUseDefaultConfigResource()
    {
        return useDefaultConfigResource;
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
    public void setDomainContext(MuleContext domainContext)
    {
        this.domainContext = domainContext.getRegistry().get("springApplicationContext");
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.springframework.context.annotation.AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.config.spring.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.spring.processors.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.spring.processors.LifecycleStatePostProcessor;
import org.mule.runtime.config.spring.processors.MuleInjectorProcessor;
import org.mule.runtime.config.spring.processors.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.spring.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.registry.MuleRegistryHelper;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context
 * that allows resources to be loaded from the Classpath of file system using the
 * MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractXmlApplicationContext
{
    private static final ThreadLocal<MuleContext> currentMuleContext = new ThreadLocal<>();

    private MuleContext muleContext;
    private Resource[] springResources;
    private final OptionalObjectsController optionalObjectsController;

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param muleContext     the {@link MuleContext} that own this context
     * @param configResources the configuration resources to use
     * @see org.mule.runtime.config.spring.SpringRegistry
     */
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] configResources)
            throws BeansException
    {
        this(muleContext, convert(configResources));
    }

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param muleContext               the {@link MuleContext} that own this context
     * @param configResources           the configuration resources to use
     * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null}
     * @see org.mule.runtime.config.spring.SpringRegistry
     * @since 3.7.0
     */
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] configResources, OptionalObjectsController optionalObjectsController)
            throws BeansException
    {
        this(muleContext, convert(configResources), optionalObjectsController);
    }

    public MuleArtifactContext(MuleContext muleContext, Resource[] springResources) throws BeansException
    {
        this(muleContext, springResources, new DefaultOptionalObjectsController());
    }

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param muleContext               the {@link MuleContext} that own this context
     * @param springResources           the configuration resources to use
     * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null}
     * @see org.mule.runtime.config.spring.SpringRegistry
     * @since 3.7.0
     */
    public MuleArtifactContext(MuleContext muleContext, Resource[] springResources, OptionalObjectsController optionalObjectsController) throws BeansException
    {
        checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
        this.muleContext = muleContext;
        this.springResources = springResources;
        this.optionalObjectsController = optionalObjectsController;
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)
    {
        super.prepareBeanFactory(beanFactory);

        registerEditors(beanFactory);

        addBeanPostProcessors(beanFactory,
                              new MuleContextPostProcessor(muleContext),
                              new GlobalNamePostProcessor(),
                              new PostRegistrationActionsPostProcessor(this, (MuleRegistryHelper) muleContext.getRegistry()),
                              new DiscardedOptionalBeanPostProcessor(optionalObjectsController, (DefaultListableBeanFactory) beanFactory),
                              new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState())
        );

        beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);
    }

    private void registerEditors(ConfigurableListableBeanFactory beanFactory)
    {
        MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
        registrar.setMuleContext(muleContext);
        beanFactory.addPropertyEditorRegistrar(registrar);
    }

    private void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors)
    {
        for (BeanPostProcessor processor : processors)
        {
            beanFactory.addBeanPostProcessor(processor);
        }
    }

    private static Resource[] convert(ConfigResource[] resources)
    {
        Resource[] configResources = new Resource[resources.length];
        for (int i = 0; i < resources.length; i++)
        {
            ConfigResource resource = resources[i];
            if (resource.getUrl() != null)
            {
                configResources[i] = new UrlResource(resource.getUrl());
            }
            else
            {
                try
                {
                    configResources[i] = new ByteArrayResource(IOUtils.toByteArray(resource.getInputStream()), resource.getResourceName());
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return configResources;
    }

    @Override
    protected Resource[] getConfigResources()
    {
        return springResources;
    }

    @Override
    protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException
    {
        BeanDefinitionReader beanDefinitionReader = createBeanDefinitionReader(beanFactory);
        // Communicate mule context to parsers
        try
        {
            currentMuleContext.set(muleContext);
            beanDefinitionReader.loadBeanDefinitions(springResources);
        }
        finally
        {
            currentMuleContext.remove();
        }
    }

    protected BeanDefinitionReader createBeanDefinitionReader(DefaultListableBeanFactory beanFactory)
    {
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        // annotate parsed elements with metadata
        beanDefinitionReader.setDocumentLoader(createLoader());
        // hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(getBeanDefinitionDocumentReaderClass());
        // add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
        registerAnnotationConfigProcessors(beanDefinitionReader.getRegistry(), null);

        return beanDefinitionReader;
    }

    protected MuleDocumentLoader createLoader()
    {
        return new MuleDocumentLoader();
    }

    private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, Object source)
    {
        registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, ConfigurationClassPostProcessor.class, source);
        registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME, RequiredAnnotationBeanPostProcessor.class, source);
        registerInjectorProcessor(registry);
    }

    protected void registerInjectorProcessor(BeanDefinitionRegistry registry)
    {
        registerAnnotationConfigProcessor(registry, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, MuleInjectorProcessor.class, null);
    }

    private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, String key, Class<?> type, Object source)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
        beanDefinition.setSource(source);
        registerPostProcessor(registry, beanDefinition, key);
    }

    protected void registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName)
    {
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(beanName, definition);
    }

    protected Class<? extends MuleBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass()
    {
        return MuleBeanDefinitionDocumentReader.class;
    }

    @Override
    protected DefaultListableBeanFactory createBeanFactory()
    {
        //Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory(getInternalParentBeanFactory());
        beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
        beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(), optionalObjectsController));

        return beanFactory;
    }

    /**
     * {@inheritDoc}
     * This implementation returns {@code false} if the
     * context hasn't been initialised yet, in opposition
     * to the default implementation which throws
     * an exception
     */
    @Override
    public boolean isRunning()
    {
        try
        {
            return super.isRunning();
        }
        catch (IllegalStateException e)
        {
            return false;
        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    protected OptionalObjectsController getOptionalObjectsController()
    {
        return optionalObjectsController;
    }

    public static ThreadLocal<MuleContext> getCurrentMuleContext()
    {
        return currentMuleContext;
    }
}

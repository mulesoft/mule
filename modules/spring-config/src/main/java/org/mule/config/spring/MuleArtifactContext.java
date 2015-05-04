/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static org.mule.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.springframework.context.annotation.AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.COMMON_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;
import org.mule.api.MuleContext;
import org.mule.config.ConfigResource;
import org.mule.config.spring.editors.MulePropertyEditorRegistrar;
import org.mule.config.spring.processors.AnnotatedTransformerObjectPostProcessor;
import org.mule.config.spring.processors.DiscardedOptionalBeanPostProcessor;
import org.mule.config.spring.processors.ExpressionEnricherPostProcessor;
import org.mule.config.spring.processors.FilteringCommonAnnotationBeanPostProcessor;
import org.mule.config.spring.processors.LifecycleStatePostProcessor;
import org.mule.config.spring.processors.NoDevkitInjectorProcessor;
import org.mule.config.spring.processors.PostRegistrationActionsPostProcessor;
import org.mule.config.spring.util.LaxInstantiationStrategyWrapper;
import org.mule.registry.MuleRegistryHelper;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
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
    private OptionalObjectsController optionalObjectsController = new OptionalObjectsController();

    /**
     * Parses configuration files creating a spring ApplicationContext which is used
     * as a parent registry using the SpringRegistry registry implementation to wraps
     * the spring ApplicationContext
     *
     * @param configResources
     * @see org.mule.config.spring.SpringRegistry
     */
    public MuleArtifactContext(MuleContext muleContext, ConfigResource[] configResources)
            throws BeansException
    {
        this(muleContext, convert(configResources));
    }

    public MuleArtifactContext(MuleContext muleContext, Resource[] springResources) throws BeansException
    {
        this.muleContext = muleContext;
        this.springResources = springResources;
    }

    @Override
    protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)
    {
        super.prepareBeanFactory(beanFactory);

        registerEditors(beanFactory);

        addBeanPostProcessors(beanFactory,
                              new MuleContextPostProcessor(muleContext),
                              new ExpressionEvaluatorPostProcessor(muleContext),
                              new GlobalNamePostProcessor(),
                              new ExpressionEnricherPostProcessor(muleContext),
                              new AnnotatedTransformerObjectPostProcessor(muleContext),
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
        //hook in our custom hierarchical reader
        beanDefinitionReader.setDocumentReaderClass(getBeanDefinitionDocumentReaderClass());
        //add error reporting
        beanDefinitionReader.setProblemReporter(new MissingParserProblemReporter());
        registerAnnotationConfigProcessors(beanDefinitionReader.getRegistry(), null);

        return beanDefinitionReader;
    }

    private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, Object source)
    {
        registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, ConfigurationClassPostProcessor.class, source);
        registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME, RequiredAnnotationBeanPostProcessor.class, source);
        registerInjectorProcessor(registry);
        registerJsr250PostProcessors(registry, source);
    }

    protected void registerInjectorProcessor(BeanDefinitionRegistry registry)
    {
        registerAnnotationConfigProcessor(registry, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME, NoDevkitInjectorProcessor.class, null);
    }

    private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, String key, Class<?> type, Object source)
    {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
        beanDefinition.setSource(source);
        registerPostProcessor(registry, beanDefinition, key);
    }

    private void registerJsr250PostProcessors(BeanDefinitionRegistry registry, Object source)
    {
        RootBeanDefinition def = new RootBeanDefinition(FilteringCommonAnnotationBeanPostProcessor.class);
        ConstructorArgumentValues arguments = new ConstructorArgumentValues();
        Set<String> filteredPackages = new HashSet<>();
        filteredPackages.add("org.apache.cxf");
        arguments.addGenericArgumentValue(filteredPackages);
        def.setConstructorArgumentValues(arguments);
        def.setSource(source);
        registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME);
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
        DefaultListableBeanFactory beanFactory = new MuleBeanFactory(getInternalParentBeanFactory());
        beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(), optionalObjectsController));

        if (getParent() != null)
        {
            //Copy over all processors
            AbstractBeanFactory parentBeanFactory = (AbstractBeanFactory) getParent().getAutowireCapableBeanFactory();
            beanFactory.copyConfigurationFrom(parentBeanFactory);
        }
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

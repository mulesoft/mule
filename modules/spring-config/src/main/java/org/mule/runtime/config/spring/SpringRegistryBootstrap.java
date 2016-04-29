/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.config.spring.factories.BootstrapObjectFactoryBean;
import org.mule.runtime.config.spring.factories.ConstantFactoryBean;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.registry.Registry;
import org.mule.runtime.core.api.registry.RegistryProvider;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.config.bootstrap.AbstractRegistryBootstrap;
import org.mule.runtime.core.config.bootstrap.BootstrapObjectFactory;
import org.mule.runtime.core.config.bootstrap.ObjectBootstrapProperty;
import org.mule.runtime.core.config.bootstrap.SimpleRegistryBootstrap;
import org.mule.runtime.core.config.bootstrap.TransformerBootstrapProperty;
import org.mule.runtime.core.transformer.TransformerUtils;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Specialization of {@link SimpleRegistryBootstrap which instead of registering the objects directly
 * into a {@link Registry}, generates {@link BeanDefinition}s into a {@link BeanDefinitionRegistry} so
 * that the Spring framework can create those objects when initialising an {@link ApplicationContext}}
 *
 * @since 3.7.0
 */
public class SpringRegistryBootstrap extends AbstractRegistryBootstrap implements BeanFactoryAware, ApplicationContextAware
{

    private OptionalObjectsController optionalObjectsController;
    private BeanDefinitionRegistry beanDefinitionRegistry;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        checkArgument(beanFactory instanceof BeanDefinitionRegistry, "this bootstrap class only accepts BeanFactory instances which implement " + BeanDefinitionRegistry.class);
        beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        if (applicationContext instanceof MuleArtifactContext)
        {
            optionalObjectsController = ((MuleArtifactContext) applicationContext).getOptionalObjectsController();
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        try
        {
            absorbAndDiscardOtherRegistries();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    protected void registerTransformers() throws MuleException
    {
        //no-op .. will happen on post processors
    }

    @Override
    protected void doRegisterTransformer(TransformerBootstrapProperty bootstrapProperty, Class<?> returnClass, Class<? extends Transformer> transformerClass) throws Exception
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(transformerClass);

        DataType returnType = null;

        if (returnClass != null)
        {
            returnType = DataTypeFactory.create(returnClass, bootstrapProperty.getMimeType());
            builder.addPropertyValue("returnDataType", returnType);
        }

        String name = bootstrapProperty.getName();
        if (name == null)
        {
            // Prefixes the generated default name to ensure there is less chance of conflict if the user registers
            // the transformer with the same name
            name = "_" + TransformerUtils.generateTransformerName(transformerClass, returnType);
        }

        builder.addPropertyValue("name", name);

        notifyIfOptional(name, bootstrapProperty.getOptional());
        doRegisterObject(name, builder);
    }

    /**
     * We want the SpringRegistry to be the only default one. This method
     * looks for other registries and absorbs its objects into the created
     * {@code beanDefinitionRegistry}. Then, the absorbed registry is unregistered
     * from the context
     */
    private void absorbAndDiscardOtherRegistries()
    {
        if (!(muleContext.getRegistry() instanceof RegistryProvider))
        {
            return;
        }

        for (Registry registry : ((RegistryProvider) muleContext.getRegistry()).getRegistries())
        {
            if (registry instanceof SpringRegistry)
            {
                continue;
            }

            for (Entry<String, Object> entry : registry.lookupByType(Object.class).entrySet())
            {
                registerInstance(entry.getKey(), entry.getValue());
            }

            muleContext.removeRegistry(registry);
        }
    }

    @Override
    protected void doRegisterObject(ObjectBootstrapProperty bootstrapProperty) throws Exception
    {
        notifyIfOptional(bootstrapProperty.getKey(), bootstrapProperty.getOptional());

        Class<?> clazz = bootstrapProperty.getService().forName(bootstrapProperty.getClassName());
        BeanDefinitionBuilder builder;

        if (BootstrapObjectFactory.class.isAssignableFrom(clazz))
        {
            final Object value = bootstrapProperty.getService().instantiateClass(bootstrapProperty.getClassName());
            builder = BeanDefinitionBuilder.rootBeanDefinition(BootstrapObjectFactoryBean.class);
            builder.addConstructorArgValue(value);
        }
        else
        {
            builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
        }

        doRegisterObject(bootstrapProperty.getKey(), builder);
    }

    private void notifyIfOptional(String key, boolean optional)
    {
        if (optional && optionalObjectsController != null)
        {
            optionalObjectsController.registerOptionalKey(key);
        }
    }

    private void doRegisterObject(String key, BeanDefinitionBuilder builder)
    {
        beanDefinitionRegistry.registerBeanDefinition(key, builder.getBeanDefinition());
    }

    private void registerInstance(String key, Object value)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ConstantFactoryBean.class);
        builder.addConstructorArgValue(value);
        doRegisterObject(key, builder);
    }

    protected OptionalObjectsController getOptionalObjectsController()
    {
        return optionalObjectsController;
    }
}

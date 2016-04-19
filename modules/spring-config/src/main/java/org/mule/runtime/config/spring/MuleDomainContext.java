/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.springframework.context.annotation.AnnotationConfigUtils.AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.config.spring.processors.ContextExclusiveInjectorProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.ConfigResource;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * A specialization of {@link MuleArtifactContext} for domains
 *
 * @since 3.6.0
 */
final class MuleDomainContext extends MuleArtifactContext
{

    MuleDomainContext(MuleContext muleContext, ConfigResource[] configResources) throws BeansException
    {
        super(muleContext, configResources);
    }

    MuleDomainContext(MuleContext muleContext, ConfigResource[] configResources, OptionalObjectsController optionalObjectsController) throws BeansException
    {
        super(muleContext, configResources, optionalObjectsController);
    }

    @Override
    protected Class<MuleDomainBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass()
    {
        return MuleDomainBeanDefinitionDocumentReader.class;
    }

    @Override
    protected void registerInjectorProcessor(BeanDefinitionRegistry registry)
    {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ContextExclusiveInjectorProcessor.class);
        builder.addConstructorArgValue(this);
        registerPostProcessor(registry, (RootBeanDefinition) builder.getBeanDefinition(), AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME);
    }

}

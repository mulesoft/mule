/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import org.mule.module.db.internal.domain.executor.SelectExecutorFactory;
import org.mule.module.db.internal.metadata.SelectMetadataProvider;
import org.mule.module.db.internal.processor.SelectMessageProcessor;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public class SelectProcessorDefinitionParser extends AbstractResultSetHandlerProcessorDefinitionParser
{

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return SelectMessageProcessor.class;
    }

    @Override
    protected Object createExecutorFactory(Element element)
    {
        BeanDefinitionBuilder executorFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(SelectExecutorFactory.class);

        executorFactoryBean.addConstructorArgValue(parseStatementFactory(element));

        executorFactoryBean.addConstructorArgReference(resultSetHandlerBeanName);


        return executorFactoryBean.getBeanDefinition();
    }

    @Override
    protected Object getMetadataProvider()
    {
        BeanDefinitionBuilder metadataProviderBuilder = BeanDefinitionBuilder.genericBeanDefinition(SelectMetadataProvider.class);
        metadataProviderBuilder.addConstructorArgValue(dbConfigResolverFactoryBeanDefinition);
        metadataProviderBuilder.addConstructorArgValue(queryBean);
        metadataProviderBuilder.addConstructorArgValue(streaming);

        return metadataProviderBuilder.getBeanDefinition();
    }

}

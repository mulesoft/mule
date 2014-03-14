/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.processor;

import org.mule.module.db.domain.executor.BulkUpdateExecutorFactory;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

public abstract class AbstractBulkUpdateProcessorBeanDefinitionParser extends AbstractAdvancedDbProcessorDefinitionParser
{

    @Override
    protected Object createExecutorFactory(Element element)
    {
        BeanDefinitionBuilder executorFactoryBean = BeanDefinitionBuilder.genericBeanDefinition(BulkUpdateExecutorFactory.class);

        executorFactoryBean.addConstructorArgValue(parseStatementFactory(element));

        return executorFactoryBean.getBeanDefinition();
    }
}

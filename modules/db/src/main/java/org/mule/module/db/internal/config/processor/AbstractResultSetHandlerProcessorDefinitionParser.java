/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.module.db.internal.result.resultset.IteratorResultSetHandler;
import org.mule.module.db.internal.result.resultset.ListResultSetHandler;
import org.mule.module.db.internal.result.row.InsensitiveMapRowHandler;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractResultSetHandlerProcessorDefinitionParser extends AbstractSingleQueryProcessorDefinitionParser
{

    protected String resultSetHandlerBeanName;

    @Override
    protected void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        resultSetHandlerBeanName = getBeanName(element) + ".resultSetHandler";
        super.doParseElement(element, context, builder);
        builder.addConstructorArgValue(streaming);
    }

    @Override
    protected void processStreamingAttribute(String streamingValue)
    {
        super.processStreamingAttribute(streamingValue);

        InsensitiveMapRowHandler recordHandler = new InsensitiveMapRowHandler();

        BeanDefinition beanDefinition;
        if (Boolean.parseBoolean(streamingValue))
        {
            beanDefinition = genericBeanDefinition(IteratorResultSetHandler.class)
                                                  .addConstructorArgValue(recordHandler)
                                                  .addConstructorArgReference("db.statementStreamingResultSetCloser")
                                                  .getBeanDefinition();
        }
        else
        {
            beanDefinition = genericBeanDefinition(ListResultSetHandler.class).addConstructorArgValue(recordHandler).getBeanDefinition();
        }

        getRegistry().registerBeanDefinition(resultSetHandlerBeanName, beanDefinition);
    }
}

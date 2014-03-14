/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.config.processor;

import org.mule.module.db.config.domain.query.BulkQueryResolverFactoryBean;
import org.mule.module.db.metadata.BulkUpdateMetadataProvider;
import org.mule.module.db.parser.SimpleQueryTemplateParser;
import org.mule.module.db.processor.BulkUpdateMessageProcessor;
import org.mule.module.db.util.DefaultFileReader;
import org.mule.module.db.resolver.query.FileBulkQueryResolver;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class BulkUpdateProcessorBeanDefinitionParser extends AbstractBulkUpdateProcessorBeanDefinitionParser
{

    @Override
    protected Class<?> getBeanClass(Element element)
    {
        return BulkUpdateMessageProcessor.class;
    }

    @Override
    protected Object getMetadataProvider()
    {
        BeanDefinitionBuilder metadataProviderBuilder = BeanDefinitionBuilder.genericBeanDefinition(BulkUpdateMetadataProvider.class);

        return metadataProviderBuilder.getBeanDefinition();
    }

    @Override
    protected void doParseElement(Element element, ParserContext context, BeanDefinitionBuilder builder)
    {
        parseConfig(element, builder);

        parseBulkQuery(element, builder);

        parseSourceExpression(element, builder);
        parseTargetExpression(element, builder);
        parseExecutorFactory(element, builder);
        parseTransactionalAction(element, builder);
        parseMetadataProvider(element, builder);
    }

    private void parseBulkQuery(Element element, BeanDefinitionBuilder builder)
    {
        String file = element.getAttribute("file");
        String queryText = element.getTextContent();

        if (StringUtils.isEmpty(file))
        {
            BeanDefinitionBuilder sqlParamResolverFactory = BeanDefinitionBuilder.genericBeanDefinition(BulkQueryResolverFactoryBean.class);
            sqlParamResolverFactory.addConstructorArgValue(queryText);
            builder.addConstructorArgValue(sqlParamResolverFactory.getBeanDefinition());
        }
        else
        {
            builder.addConstructorArgValue(new FileBulkQueryResolver(file, new SimpleQueryTemplateParser(), new DefaultFileReader()));
        }
    }
}

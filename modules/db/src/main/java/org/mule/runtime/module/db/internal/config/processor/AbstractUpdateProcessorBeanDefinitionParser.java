/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.processor;

import org.mule.module.db.internal.domain.query.QueryType;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public abstract class AbstractUpdateProcessorBeanDefinitionParser implements BeanDefinitionParser
{

    public static final String BULK_MODE_ATTRIBUTE = "bulkMode";

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        BeanDefinitionParser beanDefinitionParser;

        List<QueryType> validQueryTypes = getQueryType();

        if (element.hasAttribute(BULK_MODE_ATTRIBUTE) && element.getAttribute(BULK_MODE_ATTRIBUTE).equals("true"))
        {
            beanDefinitionParser = new PreparedBulkUpdateProcessorBeanDefinitionParser(validQueryTypes);
        }
        else
        {
            beanDefinitionParser = new SingleUpdateProcessorDefinitionParser(validQueryTypes);
        }

        return beanDefinitionParser.parse(element, parserContext);
    }

    protected abstract List<QueryType> getQueryType();
}

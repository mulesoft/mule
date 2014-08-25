/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Use this decorator to mark a {@link org.springframework.beans.factory.xml.BeanDefinitionParser} as deprecated.
 */
public class DeprecatedBeanDefinitionParser implements BeanDefinitionParser
{

    private final Logger logger;

    private final BeanDefinitionParser delegate;
    private final String deprecationMessage;

    public DeprecatedBeanDefinitionParser(BeanDefinitionParser delegate, String deprecationMessage)
    {
        this.delegate = delegate;
        this.deprecationMessage = deprecationMessage;
        this.logger = LoggerFactory.getLogger(delegate.getClass());
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext)
    {
        logger.warn(deprecationMessage);
        return delegate.parse(element, parserContext);
    }
}

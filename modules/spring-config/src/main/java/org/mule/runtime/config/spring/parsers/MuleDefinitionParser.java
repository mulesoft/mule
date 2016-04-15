/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This is the interface all Mule BDPs implement. It is a bit odd because it had to be retro-fitted
 * to existing code. In particular {@link BeanDefinitionParser#parse(Element, ParserContext)}
 * and {@link #muleParse(Element, ParserContext)} seem to duplicate each other. This is because 
 * many Mule classes subclass a Spring helper which makes <code>parse()</code> final. So instead 
 * we need to use {@link #muleParse(Element, ParserContext)}, to allow over-rides.
 * <p>
 * In case that's not clear - always call {@link #muleParse(Element, ParserContext)} rather than 
 * {@link BeanDefinitionParser#parse(Element, ParserContext)}. The {@link BeanDefinitionParser} 
 * is here only to allow the BDP to be handed over to Spring.
 */
public interface MuleDefinitionParser extends BeanDefinitionParser, MuleDefinitionParserConfiguration
{

    AbstractBeanDefinition muleParse(Element element, ParserContext parserContext);

    String getBeanName(Element element);
}

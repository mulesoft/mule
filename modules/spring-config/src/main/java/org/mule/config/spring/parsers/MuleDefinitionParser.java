/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    void setDeprecationWarning(String deprecationWarning);
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This interface allows {@link org.mule.config.spring.parsers.delegate.AbstractParallelDelegatingDefinitionParser}
 * to forward the work of parsing to a particular sub-parser.  We exploit the fact that
 * (nearly?) all parsers subclass Spring's {@link org.springframework.beans.factory.xml.AbstractBeanDefinitionParser}
 * via {@link org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser} and so provide
 * these methods
 * ({@link #parseDelegate(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)}
 * has the same signature as
 * {@link org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#parseInternal(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)}
 * and so can be delegated).
 */
public interface MuleDefinitionParser extends BeanDefinitionParser
{

    AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext);

    /**
     * These are prepended to existing processors
     * @param preProcessor
     */
    void registerPreProcessor(PreProcessor preProcessor);

    /**
     * These are appended to existing processors
     * @param postProcessor
     */
    void registerPostProcessor(PostProcessor postProcessor);

    MuleDefinitionParser addReference(String propertyName);

    MuleDefinitionParser addMapping(String propertyName, Map mappings);

    MuleDefinitionParser addMapping(String propertyName, String mappings);

    MuleDefinitionParser addAlias(String alias, String propertyName);

    MuleDefinitionParser addCollection(String propertyName);

    MuleDefinitionParser addIgnored(String propertyName);

    MuleDefinitionParser removeIgnored(String propertyName);

    MuleDefinitionParser setIgnoredDefault(boolean ignoreAll);

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * This interface allows {@link AbstractParallelDelegatingDefinitionParser}
 * to forward the work of parsing to a particular sub-parser.  We exploit the fact that
 * (nearly?) all parsers subclass Spring's {@link org.springframework.beans.factory.xml.AbstractBeanDefinitionParser}
 * via {@link org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser} and so provide
 * these methods
 * ({@link #parseDelegate(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)}
 * has the same signature as
 * {@link org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#parseInternal(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)}
 * and so can be delegated).
 */
public interface DelegateDefinitionParser
{

    AbstractBeanDefinition parseDelegate(Element element, ParserContext parserContext);

    AbstractMuleBeanDefinitionParser addReference(String propertyName);

    AbstractMuleBeanDefinitionParser addMapping(String propertyName, Map mappings);

    AbstractMuleBeanDefinitionParser addMapping(String propertyName, String mappings);

    AbstractMuleBeanDefinitionParser addAlias(String alias, String propertyName);

    AbstractMuleBeanDefinitionParser addCollection(String propertyName);

    AbstractMuleBeanDefinitionParser addIgnored(String propertyName);

    AbstractMuleBeanDefinitionParser removeIgnored(String propertyName);

    AbstractMuleBeanDefinitionParser setIgnoredDefault(boolean ignoreAll);

}

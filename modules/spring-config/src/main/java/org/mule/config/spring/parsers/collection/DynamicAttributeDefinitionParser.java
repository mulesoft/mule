/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.collection;

import org.mule.config.spring.parsers.MuleDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;

public interface DynamicAttributeDefinitionParser extends MuleDefinitionParser
{

    void setAttributeName(String attributeName);

    /**
     * This is implemented in {@link org.mule.config.spring.parsers.AbstractHierarchicalDefinitionParser}
     * and allows us to "fake" a parent when processing the same element.
     */
    void forceParent(BeanDefinition parent);

}

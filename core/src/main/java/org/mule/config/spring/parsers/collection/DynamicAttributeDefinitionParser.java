/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

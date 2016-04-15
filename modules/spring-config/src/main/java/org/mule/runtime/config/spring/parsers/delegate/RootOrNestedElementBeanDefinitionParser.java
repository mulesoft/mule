/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.delegate;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

/**
 * Bean definition parser that allows mapping an element either as root or nested inside other element.
 * Depending on the context, it delegates to a {@code MuleOrphanDefinitionParser} or to a
 * {@code ChildDefinitionParser}.
 */
public class RootOrNestedElementBeanDefinitionParser extends ParentContextDefinitionParser
{

    public RootOrNestedElementBeanDefinitionParser(Class<?> beanClass, String setterMethod)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(beanClass, true));
        otherwise(new ChildDefinitionParser(setterMethod, beanClass));
    }

}

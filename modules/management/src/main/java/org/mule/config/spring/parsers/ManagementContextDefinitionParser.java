/*
 * $Id:ManagementContextDefinitionParser.java 4715 2007-01-16 22:55:22Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.impl.ManagementContext;

import org.w3c.dom.Element;

/**
 * TODO
 */
public class ManagementContextDefinitionParser extends AbstractMuleSingleBeanDefinitionParser
{
    protected Class getBeanClass(Element element)
    {
        return ManagementContext.class;
    }
}

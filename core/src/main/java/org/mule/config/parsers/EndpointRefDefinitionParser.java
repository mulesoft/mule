/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.parsers;

import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * TODO
 */
public class EndpointRefDefinitionParser extends EndpointDefinitionParser
{

    public static final String ATTRIBUTE_ENDPOINT_REF = "idref";

    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        String parent = element.getAttribute(ATTRIBUTE_ENDPOINT_REF);
        if(StringUtils.isEmpty(parent))
        {
            throw new IllegalArgumentException("Atribute: " + ATTRIBUTE_ENDPOINT_REF + " must be specified for element: " + element.getNodeName());
        }
        BeanDefinitionBuilder bdb = BeanDefinitionBuilder.childBeanDefinition(parent);
        bdb.getBeanDefinition().setBeanClassName(beanClass.getName());
        return bdb;
    }
}

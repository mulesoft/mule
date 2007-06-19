/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.StringUtils;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * The EndpointRef definition parser extends the {@link EndpointDefinitionParser} to
 * process refernces to global endpoints.
 *
 * @see EndpointDefinitionParser
 */
public class EndpointRefDefinitionParser extends EndpointDefinitionParser
{

    protected BeanDefinitionBuilder createBeanDefinitionBuilder(Element element, Class beanClass)
    {
        String parent = element.getAttribute(ATTRIBUTE_IDREF);
        if(StringUtils.isEmpty(parent))
        {
            throw new IllegalArgumentException("Atribute: " + ATTRIBUTE_IDREF + " must be specified for element: " + element.getNodeName());
        }
        BeanDefinitionBuilder bdb = BeanDefinitionBuilder.childBeanDefinition(parent);
        bdb.getBeanDefinition().setBeanClassName(beanClass.getName());
        //need to overload the type so it becomes a local endpoint
        bdb.addPropertyValue("type", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER);
        return bdb;
    }
}

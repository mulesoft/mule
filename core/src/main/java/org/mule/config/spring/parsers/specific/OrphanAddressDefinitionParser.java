/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

/**
 * Generate an Endpoint URI from simple address components.
 */
public class OrphanAddressDefinitionParser extends OrphanDefinitionParser
{

    private String protocol;

    public OrphanAddressDefinitionParser(String protocol)
    {
        super(LazyEndpointURI.class, true);
        this.protocol = protocol;
    }

    // @Override
    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        super.postProcess(builder, element);
        getOrphanBeanAssembler(builder).extendBean("protocol", protocol, false);
    }

}
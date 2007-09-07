/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.assembly.BeanAssembler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;

import org.w3c.dom.Element;

/**
 * Generate an Endpoint URI from simple address components.
 */
public class ChildAddressDefinitionParser extends ChildDefinitionParser
{

    private String protocol;

    public ChildAddressDefinitionParser(String protocol)
    {
        super("endpointURI", LazyEndpointURI.class);
        this.protocol = protocol;
    }

    // @Override
    protected void postProcess(BeanAssembler assembler, Element element)
    {
        assembler.extendBean("protocol", protocol, false);
        super.postProcess(assembler, element);
    }

}

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

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.impl.endpoint.MuleEndpointURI;

import java.util.Properties;
import java.net.URI;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

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
    protected void postProcess(BeanDefinitionBuilder builder, Element element)
    {
        super.postProcess(builder, element);
        getBeanAssembler(element, builder).extendBean("protocol", protocol, false);
    }

}

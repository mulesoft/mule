/*
 * $Id:TransportEndpointDefinitionParser.java 8321 2007-09-10 19:22:52Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;

/**
 * This is intended for use by endpoint-specific parsers for non-global endpoint
 * elements.  It will not allow the "ref" attribute with any of the
 * {@link org.mule.config.spring.parsers.specific.LazyEndpointURI#ALL_ATTRIBUTES}.
 *
 * <p>It generates both an endpoint (which should subclass
 * {@link org.mule.impl.endpoint.MuleEndpoint}) and a
 * {@link org.mule.config.spring.parsers.specific.LazyEndpointURI}.  The URI is
 * then injected into the endpoint.  So the associated schema can enable any of the
 * suitable {@link org.mule.config.spring.parsers.specific.LazyEndpointURI# ALL_ATTRIBUTES}
 * or add appropriate mappings.
 */
public class TransportEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportEndpointDefinitionParser(String protocol, Class endpoint, String[] requiredAttributes)
    {
        this(protocol, PROTOCOL, endpoint, requiredAttributes);
    }

    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] requiredAttributes)
    {
        this(metaOrProtocol, isMeta, endpoint, new String[]{}, requiredAttributes);
    }

    /**
     * @param metaOrProtocol The transport protocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param endpoint The endpoint class
     * @param properties A list of attribute names which will be set as properties on the
     * endpointParser
     * @param requiredAttributes These are the subset of the alternate address attributes that must be
     * specified if "address" isn't defined.
     */
    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] properties, String[] requiredAttributes)
    {
        super(metaOrProtocol, isMeta, new ChildEndpointDefinitionParser(endpoint),
                properties, requiredAttributes);
    }

}

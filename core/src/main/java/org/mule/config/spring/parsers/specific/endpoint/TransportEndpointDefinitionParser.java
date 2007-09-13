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
import org.mule.config.spring.parsers.preprocessors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;

/**
 * This is intended for use by endpoint-specific parsers for non-global endpoint
 * elements.  It will not allow the "ref" attribute with any of the
 * {@link org.mule.config.spring.parsers.specific.LazyEndpointURI#ATTRIBUTES}.
 *
 * <p>It generates both an endpoint (which should subclass
 * {@link org.mule.impl.endpoint.MuleEndpoint}) and a
 * {@link org.mule.config.spring.parsers.specific.LazyEndpointURI}.  The URI is
 * then injected into the endpoint.  So the associated schema can enable any of the
 * suitable {@link org.mule.config.spring.parsers.specific.LazyEndpointURI#ATTRIBUTES}
 * or add appropriate mappings.
 */
public class TransportEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportEndpointDefinitionParser(String protocol, Class endpoint)
    {
        super(protocol, new ChildEndpointDefinitionParser(endpoint));
        registerPreProcessor(
                new CheckExclusiveAttributes(new String[][]{
                        new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF},
                        LazyEndpointURI.ATTRIBUTES}));
    }

}

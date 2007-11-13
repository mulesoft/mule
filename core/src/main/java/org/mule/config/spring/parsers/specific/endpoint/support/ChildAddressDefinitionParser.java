/*
 * $Id:ChildAddressDefinitionParser.java 8321 2007-09-10 19:22:52Z acooke $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.URIBuilder;
import org.mule.config.spring.parsers.processors.AddAttribute;

/**
 * Generate an Endpoint URI from simple address components.
 */
public class ChildAddressDefinitionParser extends ChildDefinitionParser
{

    public static final boolean META = true;
    public static final boolean PROTOCOL = false;

    public ChildAddressDefinitionParser(String protocol)
    {
        this(protocol, PROTOCOL);
    }

    public ChildAddressDefinitionParser(String metaOrProtocol, boolean isMeta)
    {
        super(EndpointUtils.ENDPOINT_BUILDER_ATTRIBUTE, URIBuilder.class);
        registerPreProcessor(
                new AddAttribute(
                        isMeta ? URIBuilder.META : URIBuilder.PROTOCOL,
                        metaOrProtocol));
    }

}

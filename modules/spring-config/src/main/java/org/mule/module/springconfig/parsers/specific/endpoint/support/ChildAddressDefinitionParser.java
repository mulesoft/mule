/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific.endpoint.support;

import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.processors.AddAttribute;
import org.mule.endpoint.URIBuilder;

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
        super(EndpointUtils.URI_BUILDER_ATTRIBUTE, URIBuilder.class);
        registerPreProcessor(
                new AddAttribute(
                        isMeta ? URIBuilder.META : URIBuilder.PROTOCOL,
                        metaOrProtocol));
    }

}

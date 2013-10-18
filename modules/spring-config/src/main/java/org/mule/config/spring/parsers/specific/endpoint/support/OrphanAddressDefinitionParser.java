/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.AddAttribute;
import org.mule.endpoint.URIBuilder;

/**
 * Generate an Endpoint URI from simple address components.
 */
public class OrphanAddressDefinitionParser extends OrphanDefinitionParser
{

    public static final boolean META = true;
    public static final boolean PROTOCOL = false;

    public OrphanAddressDefinitionParser(String metaOrProtocol, boolean isMeta)
    {
        super(URIBuilder.class, true);
        registerPreProcessor(
                new AddAttribute(
                        isMeta ? URIBuilder.META : URIBuilder.PROTOCOL,
                        metaOrProtocol));
    }

}

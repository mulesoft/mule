/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;

/**
 * A parser for global endpoints.  Note that the blocking of "ref" is left to the schema.
 */
public class TransportGlobalEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportGlobalEndpointDefinitionParser(String protocol, String[] requiredAddressAttributes)
    {
        this(protocol, PROTOCOL, requiredAddressAttributes);
    }

    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, String[] requiredAddressAttributes)
    {
        this(metaOrProtocol, isMeta, requiredAddressAttributes, new String[]{});
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param requiredAddressAttributes A list of attribute names that are required if "address"
     * isn't present
     * @param requiredProperties A list of property names that are required if "address" isn't present
     */
    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                                   String[] requiredAddressAttributes, String[] requiredProperties)
    {
        super(metaOrProtocol, isMeta, new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class),
                requiredAddressAttributes, requiredProperties);
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param requiredAddressAttributes A list of attribute names that are required if "address"
     * isn't present
     * @param requiredProperties A list of property names that are required if "address" isn't present
     */
    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class clazz,
                                                   String[] requiredAddressAttributes, String[] requiredProperties)
    {
        super(metaOrProtocol, isMeta, new OrphanEndpointDefinitionParser(clazz),
                requiredAddressAttributes, requiredProperties);
    }

    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                                   String[] endpointAttributes,
                                                   String[][] requiredAddressAttributes,
                                                   String[][] requiredProperties)
    {
        this(metaOrProtocol, isMeta, URIBuilder.ALL_ATTRIBUTES,
                endpointAttributes, requiredAddressAttributes, requiredProperties);
    }

    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                                   String[] endpointAttributes,
                                                   String[] addressAttributes,
                                                   String[][] requiredAddressAttributes,
                                                   String[][] requiredProperties)
    {
        super(metaOrProtocol, isMeta,
                new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class),
                addressAttributes, endpointAttributes, requiredAddressAttributes, requiredProperties);
    }

}

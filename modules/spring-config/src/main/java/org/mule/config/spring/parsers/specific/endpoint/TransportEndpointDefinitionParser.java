/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.endpoint.URIBuilder;

/**
 * This is intended for use by endpoint-specific parsers for non-global endpoint
 * elements.
 *
 * <p>It generates both an endpoint (which should subclass {@link ImmutableEndpoint}) and a
 * {@link URIBuilder}.  The URI is then injected into the endpoint. So the associated schema 
 * can enable any of the suitable {@link URIBuilder#ALL_ATTRIBUTES} or add appropriate mappings.
 */
public class TransportEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportEndpointDefinitionParser(String protocol, Class endpoint, String[] requiredAddressAttributes)
    {
        this(protocol, PROTOCOL, endpoint, requiredAddressAttributes);
    }

    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] requiredAddressAttributes)
    {
        this(metaOrProtocol, isMeta, endpoint, requiredAddressAttributes, new String[]{});
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param endpoint The endpoint class to construct
     * @param requiredAddressAttributes A list of attribute names that are required if "address"
     * isn't present
     * @param requiredProperties A list of property names that are required if "address" isn't present
     */
    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] requiredAddressAttributes, String[] requiredProperties)
    {
        super(metaOrProtocol, isMeta, new ChildEndpointDefinitionParser(endpoint),
                requiredAddressAttributes, requiredProperties);
    }

    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] endpointAttributes,
                                             String[][] requiredAddressAttributes,
                                             String[][] requiredProperties)
    {
        this(metaOrProtocol, isMeta, endpoint, endpointAttributes, URIBuilder.ALL_ATTRIBUTES,
                requiredAddressAttributes, requiredProperties);
    }

    public TransportEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, Class endpoint,
                                             String[] endpointAttributes,
                                             String[] addressAttributes,
                                             String[][] requiredAddressAttributes,
                                             String[][] requiredProperties)
    {
        super(metaOrProtocol, isMeta, new ChildEndpointDefinitionParser(endpoint),
                endpointAttributes, addressAttributes, requiredAddressAttributes, requiredProperties);
    }

}

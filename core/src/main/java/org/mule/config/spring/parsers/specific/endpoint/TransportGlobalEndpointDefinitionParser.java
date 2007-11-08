/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;

/**
 * A parser for global endpoints.  Note that the blocking of "ref" is left to the schema.
 */
public class TransportGlobalEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportGlobalEndpointDefinitionParser(String protocol, String[] requiredAttributes)
    {
        this(protocol, PROTOCOL, requiredAttributes);
    }

    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, String[] requiredAttributes)
    {
        this(metaOrProtocol, isMeta, new String[]{}, requiredAttributes);
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param properties A list of attribute names which will be set as properties on the
     * endpointParser
     * @param requiredAttributes These are the subset of the alternate address attributes that must be
     * specified if "address" isn't defined.
     */
    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta,
                                                   String[] properties, String[] requiredAttributes)
    {
        super(metaOrProtocol, isMeta, new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class),
                properties, requiredAttributes);
    }

}

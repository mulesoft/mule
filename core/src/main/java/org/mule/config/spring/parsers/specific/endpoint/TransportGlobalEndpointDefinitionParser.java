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

public class TransportGlobalEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportGlobalEndpointDefinitionParser(String protocol)
    {
        this(protocol, PROTOCOL);
    }

    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta)
    {
        this(metaOrProtocol, isMeta, new String[]{});
    }

    /**
     * @param metaOrProtocol The transport metaOrProtocol ("tcp" etc)
     * @param isMeta Whether transport is "meta" or not (eg cxf)
     * @param properties A list of attribute names which will be set as properties on the
     * endpointParser
     */
    public TransportGlobalEndpointDefinitionParser(String metaOrProtocol, boolean isMeta, String[] properties)
    {
        super(metaOrProtocol, isMeta, new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class), properties);
    }

}

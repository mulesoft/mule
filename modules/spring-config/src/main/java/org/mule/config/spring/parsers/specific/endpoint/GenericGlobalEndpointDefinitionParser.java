/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.endpoint;

import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.endpoint.EndpointURIEndpointBuilder;

public class GenericGlobalEndpointDefinitionParser extends OrphanEndpointDefinitionParser
{

    public GenericGlobalEndpointDefinitionParser()
    {
        super(EndpointURIEndpointBuilder.class);
    }

}

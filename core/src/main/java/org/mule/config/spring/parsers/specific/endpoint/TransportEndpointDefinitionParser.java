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

import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.LazyEndpointURI;
import org.mule.config.spring.parsers.preprocessors.CheckExclusiveAttributes;

public class TransportEndpointDefinitionParser extends AddressedEndpointDefinitionParser
{

    public TransportEndpointDefinitionParser(String protocol, Class endpoint)
    {
        super(protocol, new ChildEndpointDefinitionParser(endpoint));
        registerPreProcessor(
                new CheckExclusiveAttributes(new String[][]{
                        new String[]{"ref"},
                        LazyEndpointURI.ATTRIBUTES}));
    }

}

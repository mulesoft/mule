/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.providers.soap.xfire.XFireConnector;

public class XfireElementDefinitionParser extends MuleOrphanDefinitionParser
{
    public XfireElementDefinitionParser()
    {
        super(XFireConnector.class, true);
        this.addAlias("bindingProviderClass", "bindingProvider");
        this.addAlias("clientTransportClass", "clientTransport");
        this.addAlias("typeMappingRegistryClass", "typeMappingRegistry");
    }
}

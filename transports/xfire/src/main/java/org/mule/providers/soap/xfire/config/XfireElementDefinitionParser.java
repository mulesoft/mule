/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.config;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.providers.soap.xfire.XFireConnector;

public class XfireElementDefinitionParser extends OrphanDefinitionParser
{
    public XfireElementDefinitionParser()
    {
        super(XFireConnector.class, true);
        this.withAlias("bindingProviderClass", "bindingProvider");
        this.withAlias("clientTransportClass", "clientTransport");
        this.withAlias("typeMappingRegistryClass", "typeMappingRegistry");
    }
}

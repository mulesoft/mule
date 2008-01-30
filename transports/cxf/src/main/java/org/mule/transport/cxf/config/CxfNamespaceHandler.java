/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.transport.cxf.CxfConnector;

public class CxfNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerMetaTransportEndpoints(CxfConnector.CXF);
        registerConnectorDefinitionParser(CxfConnector.class);
        registerBeanDefinitionParser(FeaturesDefinitionParser.FEATURES, new FeaturesDefinitionParser());
    }

}

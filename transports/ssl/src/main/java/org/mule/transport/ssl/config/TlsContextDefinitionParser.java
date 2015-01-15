/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.config;

import org.mule.module.springconfig.parsers.delegate.ParentContextDefinitionParser;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.transport.ssl.DefaultTlsContextFactory;


public class TlsContextDefinitionParser extends ParentContextDefinitionParser
{

    public TlsContextDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENT, new MuleOrphanDefinitionParser(DefaultTlsContextFactory.class, true));
        otherwise(new ChildDefinitionParser("tlsContext", DefaultTlsContextFactory.class));
    }

}

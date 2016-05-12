/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.config;

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.specific.tls.KeyStoreDefinitionParser;


public class KeyStoreParentContextDefinitionParser extends ParentContextDefinitionParser
{

    public KeyStoreParentContextDefinitionParser()
    {
        super("context", new KeyStoreTlsContextDefinitionParser());
        otherwise(new KeyStoreDefinitionParser());
    }

}

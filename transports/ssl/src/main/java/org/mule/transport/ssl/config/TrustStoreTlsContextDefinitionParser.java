/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.config;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;


public class TrustStoreTlsContextDefinitionParser extends ParentDefinitionParser
{

    public TrustStoreTlsContextDefinitionParser()
    {
        addAlias("path", "trustStorePath");
        addAlias("password", "trustStorePassword");
        addAlias("type", "trustStoreType");
        addAlias("algorithm", "trustManagerAlgorithm");
        addAlias("insecure", "trustStoreInsecure");
    }

}

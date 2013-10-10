/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.tls;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

public class ClientKeyStoreDefinitionParser extends ParentDefinitionParser
{

    public ClientKeyStoreDefinitionParser()
    {
        addAlias("path", "clientKeyStore");
        addAlias("storePassword", "clientKeyStorePassword");
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, "clientKeyStoreType");
    }

}

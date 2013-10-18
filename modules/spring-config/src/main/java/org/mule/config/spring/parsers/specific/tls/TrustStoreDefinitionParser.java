/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific.tls;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;

public class TrustStoreDefinitionParser extends ParentDefinitionParser
{

    public TrustStoreDefinitionParser()
    {
        registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
                new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS}, new String[]{"type"}}));
        addAlias("path", "trustStore");
        addAlias("storePassword", "trustStorePassword");
        // these used by server trust store type
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, "trustStoreType");
        addAlias("type", "trustStoreType");
        addAlias("algorithm", "trustManagerAlgorithm");
        addAlias("factory", "trustManagerFactory");
        addAlias("explicitOnly", "explicitTrustStoreOnly");
    }

}

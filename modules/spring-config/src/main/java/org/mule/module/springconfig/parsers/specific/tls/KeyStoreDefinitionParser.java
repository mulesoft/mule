/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific.tls;

import org.mule.module.springconfig.parsers.generic.ParentDefinitionParser;
import org.mule.module.springconfig.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.module.springconfig.parsers.processors.CheckExclusiveAttributes;

public class KeyStoreDefinitionParser extends ParentDefinitionParser
{

    public KeyStoreDefinitionParser()
    {
        registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
                new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS}, new String[]{"type"}}));
        addAlias("path", "keyStore");
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, "keyStoreType");
        addAlias("type", "keyStoreType");
        addAlias("storePassword", "keyStorePassword");
        addAlias("algorithm", "keyManagerAlgorithm");
    }

}

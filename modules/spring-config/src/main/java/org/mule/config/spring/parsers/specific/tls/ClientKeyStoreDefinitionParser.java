/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers.specific.tls;

import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;

public class ClientKeyStoreDefinitionParser extends ParentDefinitionParser
{

    public ClientKeyStoreDefinitionParser()
    {
        registerPreProcessor(new CheckExclusiveAttributes(new String[][]{
                new String[]{AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS}, new String[]{"type"}}));
        addAlias("path", "clientKeyStore");
        addAlias("storePassword", "clientKeyStorePassword");
        addAlias(AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS, "clientKeyStoreType");
        addAlias("type", "clientKeyStoreType");
    }

}

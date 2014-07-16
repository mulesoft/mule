/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.BlockAttribute;

/**
 * Generates a transaction config with embedded factory.  If no factory is defined, it's taken from the
 * factory-class or factory-ref attributes.
 */
public class TransactionDefinitionParser extends AbstractSingleParentFamilyDefinitionParser
{

    public static final String FACTORY = "factory";
    public static final String FACTORY_REF = FACTORY + "-ref";
    public static final String FACTORY_CLASS = FACTORY + "-" + AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS;
    public static final String ACTION = "action";
    public static final String TIMEOUT = "timeout";
    public static final String INTERACT_WTH_EXTERNAL = "interactWithExternal";

    public TransactionDefinitionParser()
    {
        commonInit(null);
    }

    public TransactionDefinitionParser(Class factoryClass)
    {
        commonInit(factoryClass);
        MuleDefinitionParser factoryParser = getDelegate(1);
        // don't allow these if the class is specified in the constructor
        factoryParser.registerPreProcessor(new BlockAttribute(new String[]{FACTORY_CLASS, FACTORY_REF}));
    }

    private void commonInit(Class factoryClass)
    {
        addDelegate(new TransactionConfigDefinitionParser())
                .setIgnoredDefault(true)
                .removeIgnored(FACTORY_REF)
                .removeIgnored(ACTION)
                .removeIgnored(TIMEOUT)
                .removeIgnored(INTERACT_WTH_EXTERNAL);
        addDelegateAsChild(new ChildDefinitionParser(FACTORY, factoryClass))
                .setIgnoredDefault(false)
                .addIgnored(FACTORY_REF)
                .addIgnored(ACTION)
                .addIgnored(TIMEOUT)
                .addIgnored(INTERACT_WTH_EXTERNAL)
                .addAlias(FACTORY_CLASS, AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS)
                // the ref is set on the parent
                .registerPreProcessor(new BlockAttribute(FACTORY));
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addHandledException(BlockAttribute.BlockAttributeException.class);
    }

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractSingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.BlockAttribute;
import org.mule.util.ClassUtils;

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

    public TransactionDefinitionParser()
    {
        commonInit(null);
    }

    public TransactionDefinitionParser(String factoryClassName)
    {
        Class factoryClass;
        try
        {
            factoryClass = ClassUtils.loadClass(factoryClassName, getClass());
        }
        catch (ClassNotFoundException e)
        {
            if (MuleNamespaceHandler.TRANSACTION_COLLECTION_FACTORY_CLASSNAME.equals(factoryClassName))
            {
                throw new UnsupportedOperationException("Multi-transaction support is an EE-only feature. " +
                                                        "Please consider upgrading to Mule EE.");
            }
            else
            {
                throw new MuleRuntimeException(
                        MessageFactory.createStaticMessage("Failed to load factory class " + factoryClassName), e);
            }
        }
        commonInit(factoryClass);
        MuleDefinitionParser factoryParser = getDelegate(1);
        // don't allow these if the class is specified in the constructor
        factoryParser.registerPreProcessor(new BlockAttribute(new String[]{FACTORY_CLASS, FACTORY_REF}));
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
                .removeIgnored(TIMEOUT);
        addDelegateAsChild(new ChildDefinitionParser(FACTORY, factoryClass))
                .setIgnoredDefault(false)
                .addIgnored(FACTORY_REF)
                .addIgnored(ACTION)
                .addIgnored(TIMEOUT)
                .addAlias(FACTORY_CLASS, AbstractMuleBeanDefinitionParser.ATTRIBUTE_CLASS)
                // the ref is set on the parent
                .registerPreProcessor(new BlockAttribute(FACTORY));
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addHandledException(BlockAttribute.BlockAttributeException.class);
    }

}

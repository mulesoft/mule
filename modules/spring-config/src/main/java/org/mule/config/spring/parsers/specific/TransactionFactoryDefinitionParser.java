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

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.BlockAttribute;
import org.mule.config.spring.parsers.processors.RequireAttribute;

/**
 * This allows a transaction factory to be defined globally, or embedded within an endpoint.
 */
public class TransactionFactoryDefinitionParser extends ParentContextDefinitionParser
{

    public TransactionFactoryDefinitionParser(Class clazz)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS, new MuleOrphanDefinitionParser(clazz, true));
        otherwise(new ChildDefinitionParser("factory", clazz));
        setConstraints();
    }


    /**
     * For custom transformers
     */
    public TransactionFactoryDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS, new MuleOrphanDefinitionParser(true));
        otherwise(new ChildDefinitionParser("factory"));
        setConstraints();
    }


    protected void setConstraints()
    {
        // if global, must have name
        getDelegate(0).registerPreProcessor(new RequireAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME));
        getDelegate(0).addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);

        // if local, must not have name
        getOtherwise().registerPreProcessor(new BlockAttribute(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME));
    }

}
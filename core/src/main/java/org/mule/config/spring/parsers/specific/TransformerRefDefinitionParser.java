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

import org.mule.config.spring.parsers.delegate.ParentContextDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

/**
 * Handle response transformers correctly
 */
public class TransformerRefDefinitionParser extends ParentContextDefinitionParser
{

    public TransformerRefDefinitionParser()
    {
        super(TransformerDefinitionParser.RESPONSE_TRANSFORMERS,
                new ParentDefinitionParser().addAlias(
                        AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF,
                        TransformerDefinitionParser.RESPONSE_TRANSFORMER));
        otherwise(new ParentDefinitionParser().addAlias(
                        AbstractMuleBeanDefinitionParser.ATTRIBUTE_REF,
                        TransformerDefinitionParser.TRANSFORMER));
    }

}
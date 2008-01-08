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
import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

/**
 * This allows a transformer to be defined globally, or embedded within an endpoint
 * (as either a normal or response transformer).
 */
public class TransformerDefinitionParser extends ParentContextDefinitionParser
{

    public static final String TRANSFORMER = "transformer";
    public static final String RESPONSE_TRANSFORMER = "responseTransformer";
    public static final String RESPONSE_TRANSFORMERS = RESPONSE_TRANSFORMER + "s";

    public TransformerDefinitionParser(Class transformer)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS, new MuleOrphanDefinitionParser(transformer, false));
        and(RESPONSE_TRANSFORMERS, new ChildDefinitionParser(RESPONSE_TRANSFORMER, transformer));
        otherwise(new ChildDefinitionParser(TRANSFORMER, transformer));
    }

    /**
     * For custom transformers
     */
    public TransformerDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS, new MuleOrphanDefinitionParser(false));
        and(RESPONSE_TRANSFORMERS, new ChildDefinitionParser(RESPONSE_TRANSFORMER));
        otherwise(new ChildDefinitionParser(TRANSFORMER));
    }

}

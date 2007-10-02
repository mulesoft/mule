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
import org.mule.umo.UMOFilter;

/**
 * This allows a filter to be defined globally, or embedded within an endpoint.
 */
public class FilterDefinitionParser extends ParentContextDefinitionParser
{

    public static final String FILTER = "filter";
    public static final String ATTRIBUTE_NAME = AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME;

    public FilterDefinitionParser(Class filter)
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS,
                new MuleOrphanDefinitionParser(filter, false).addIgnored(ATTRIBUTE_NAME));
        otherwise(new ChildDefinitionParser(FILTER, filter, UMOFilter.class, false).addIgnored(ATTRIBUTE_NAME));
    }

    /**
     * For custom transformers
     */
    public FilterDefinitionParser()
    {
        super(MuleOrphanDefinitionParser.ROOT_ELEMENTS,
                new MuleOrphanDefinitionParser(false).addIgnored(ATTRIBUTE_NAME));
        otherwise(new ChildDefinitionParser(FILTER, null, UMOFilter.class, true).addIgnored(ATTRIBUTE_NAME));
    }

}

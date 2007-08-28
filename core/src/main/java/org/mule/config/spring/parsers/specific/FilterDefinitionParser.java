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

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.umo.UMOFilter;

/**
 * Parses all the core filter types such as logic (AND OR NOT) and generic filters such as <i>Payload Type</i>,
 * <i>RegEX</i> and <i>Message Property</i>
 * 
 */
public class FilterDefinitionParser extends ChildDefinitionParser
{

    public FilterDefinitionParser(Class clazz)
    {
        super("filter", clazz, UMOFilter.class, false);
    }

    /**
     * For custom filters that use the class attribute
     */
    public FilterDefinitionParser()
    {
        super("filter", null, UMOFilter.class, true);
    }

}
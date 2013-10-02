/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.specific;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.routing.filters.WildcardFilter;

/**
 * TODO
 */
public class ExceptionTXFilterDefinitionParser extends ChildDefinitionParser
{
    /**
     * The class will be inferred from the class attribute
     *
     * @param setterMethod The target method (where the child will be injected)
     */
    public ExceptionTXFilterDefinitionParser(String setterMethod)
    {
        super(setterMethod, WildcardFilter.class);
        addAlias("exception-pattern", "pattern");

    }
}

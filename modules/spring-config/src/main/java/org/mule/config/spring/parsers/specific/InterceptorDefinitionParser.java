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

/**
 * This allows a interceptor to be defined on a global interceptor stack or on a
 * service.
 */
public class InterceptorDefinitionParser extends ChildDefinitionParser
{

    public static final String INTERCEPTOR = "interceptor";

    public InterceptorDefinitionParser(Class interceptor)
    {
        super(INTERCEPTOR, interceptor);
    }

    /**
     * For custom transformers
     */
    public InterceptorDefinitionParser()
    {
        super(INTERCEPTOR);
    }
}

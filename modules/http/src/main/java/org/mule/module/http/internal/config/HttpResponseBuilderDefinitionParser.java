/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.module.springconfig.parsers.delegate.ParentContextDefinitionParser;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.http.internal.listener.HttpResponseBuilder;

public class HttpResponseBuilderDefinitionParser extends ParentContextDefinitionParser
{
    public HttpResponseBuilderDefinitionParser(String setterMethod)
    {
        super("listener", new ChildDefinitionParser(setterMethod, HttpResponseBuilder.class));
        otherwise(new MuleOrphanDefinitionParser(HttpResponseBuilder.class, true));
    }

}
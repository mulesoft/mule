/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;


public class HttpRequestConfigDefinitionParser extends MuleOrphanDefinitionParser
{

    public HttpRequestConfigDefinitionParser()
    {
        super(DefaultHttpRequesterConfig.class, true);
        addReference("tlsContext");
        addReference("proxyConfig");
        addAlias("proxy", "proxyConfig");
    }

}

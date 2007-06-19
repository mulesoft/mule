/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.security;

public class CustomSecurityProviderDefinitionParser extends CustomSecurityDefinitionParser
{

    public static final String PROVIDER = "provider";
    public static final String PROVIDERS = "providers";

    public CustomSecurityProviderDefinitionParser()
    {
        super(PROVIDERS);
        withAlias(PROVIDER, PROVIDERS);
        withCollection(PROVIDERS);
    }

}
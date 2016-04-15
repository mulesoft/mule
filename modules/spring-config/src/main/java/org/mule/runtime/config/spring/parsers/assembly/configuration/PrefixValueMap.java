/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly.configuration;

public class PrefixValueMap implements ValueMap
{

    private String prefix;

    public PrefixValueMap(String prefix)
    {
        this.prefix = prefix;
    }

    public Object rewrite(String value)
    {
        return prefix + value;
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers.assembly.configuration;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Be careful - this doesn't work with endpoint properties because they need to be converted to a URI.
 */
public class ListValueMap implements ValueMap
{

    public Object rewrite(String value)
    {
        List list = new LinkedList();
        StringTokenizer tokenizer = new StringTokenizer(value);
        while (tokenizer.hasMoreTokens())
        {
            list.add(tokenizer.nextToken());
        }
        return list;
    }

}

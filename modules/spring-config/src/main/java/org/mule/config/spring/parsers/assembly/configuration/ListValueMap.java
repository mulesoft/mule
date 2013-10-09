/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

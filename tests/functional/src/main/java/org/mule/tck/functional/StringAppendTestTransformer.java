/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

import org.mule.transformer.simple.StringAppendTransformer;

public class StringAppendTestTransformer extends StringAppendTransformer
{

    public static final String DEFAULT_TEXT = " transformed";

    public StringAppendTestTransformer()
    {
        setMessage(DEFAULT_TEXT);
    }

    public static String appendDefault(String msg)
    {
        return append(DEFAULT_TEXT, msg);
    }

}

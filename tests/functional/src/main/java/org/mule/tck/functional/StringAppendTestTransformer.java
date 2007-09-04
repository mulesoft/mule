/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.transformers.simple.StringAppendTransformer;

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

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

public class CharSetUtils extends org.apache.commons.lang.CharSetUtils
{
    public static String defaultCharsetName()
    {
        try
        {
            if (SystemUtils.IS_JAVA_1_4)
            {
                return new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding();
            }
            else
            {
                Class target = Charset.class;
                Method defaultCharset = target.getMethod("defaultCharset", ArrayUtils.EMPTY_CLASS_ARRAY);
                Charset cs = (Charset) defaultCharset.invoke(target, (Object[]) null);
                return cs.name();
            }
        }
        catch (Exception ex)
        {
            throw new Error(ex);
        }
    }
}



/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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



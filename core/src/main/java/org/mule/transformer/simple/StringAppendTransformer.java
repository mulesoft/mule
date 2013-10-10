/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;

public class StringAppendTransformer extends AbstractTransformer
{
    private String message = StringUtils.EMPTY;

    public StringAppendTransformer()
    {
        this(StringUtils.EMPTY);
    }

    public StringAppendTransformer(String message)
    {
        this.message = message;
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        String string;
        if (src instanceof byte[])
        {
            string = new String((byte[]) src);
        }
        else if (src instanceof InputStream)
        {
            InputStream input = (InputStream) src;
            try
            {
                string = IOUtils.toString(input);
            }
            finally
            {
                IOUtils.closeQuietly(input);
            }
        }
        else
        {
            string = (String) src;
        }

        return append(message, string);
    }

    public static String append(String append, String msg)
    {
        return msg + append;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }
}

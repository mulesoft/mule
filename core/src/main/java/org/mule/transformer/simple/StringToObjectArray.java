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
import java.io.UnsupportedEncodingException;

/**
 * <code>StringToObjectArray</code> converts a String into an object array. This
 * is useful in certain situations, as when a string needs to be converted into
 * an Object[] in order to be passed to a SOAP service. The input String is parsed
 * into the array based on a configurable delimiter - default is a space.
 */
public class StringToObjectArray extends AbstractTransformer
{
    private String delimiter = null;
    private static final String DEFAULT_DELIMITER = " ";

    public StringToObjectArray()
    {
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.create(Object[].class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        String in;

        if (src instanceof byte[])
        {
            in = createStringFromByteArray((byte[]) src, outputEncoding);
        }
        else if (src instanceof InputStream)
        {
            in = createStringFromInputStream((InputStream) src);
        }
        else
        {
            in = (String) src;
        }

        String[] out = StringUtils.splitAndTrim(in, getDelimiter());
        return out;
    }

    protected String createStringFromByteArray(byte[] bytes, String outputEncoding) throws TransformerException
    {
        try
        {
            return new String(bytes, outputEncoding);
        }
        catch (UnsupportedEncodingException uee)
        {
            throw new TransformerException(this, uee);
        }
    }

    protected String createStringFromInputStream(InputStream input)
    {
        try
        {
            return IOUtils.toString(input);
        }
        finally
        {
            IOUtils.closeQuietly(input);
        }
    }

    /**
     * @return the delimiter
     */
    public String getDelimiter()
    {
        if (delimiter == null)
        {
            return DEFAULT_DELIMITER;
        }
        else
        {
            return delimiter;
        }
    }

    /**
     * @param delimiter the delimiter
     */
    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }

}

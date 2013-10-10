/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.scripting;


import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.NumberUtils;

/**
 * Converts a string to a number.
 */
public class StringToNumber extends AbstractTransformer
{
    /** Convert the string to an integer (by default it will convert it to a double) */
    private boolean integer = false;
    
    public StringToNumber()
    {
        registerSourceType(DataTypeFactory.STRING);
        setReturnDataType(DataTypeFactory.create(Number.class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        if (integer)
        {
            return new Integer(NumberUtils.toInt(src));
        }
        else
        {
            return new Double(NumberUtils.toDouble(src));
        }
    }

    public boolean isInteger()
    {
        return integer;
    }

    public void setInteger(boolean integer)
    {
        this.integer = integer;
    }
}

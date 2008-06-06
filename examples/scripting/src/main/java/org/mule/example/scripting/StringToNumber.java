/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.scripting;


import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
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
        registerSourceType(String.class);
        setReturnClass(Number.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
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

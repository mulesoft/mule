/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

import java.text.NumberFormat;

/**
 * <code>NumberToString</code> converts a Number to a String.  A NumberFormat is used if one is provided.
 */
public class NumberToString extends AbstractTransformer implements DiscoverableTransformer
{

    private NumberFormat numberFormat;

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public NumberToString()
    {
        registerSourceType(new SimpleDataType<Object>(Number.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    public NumberToString(NumberFormat numberFormat)
    {
        this();
        this.numberFormat = numberFormat;
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            return "";
        }

        if (numberFormat != null)
        {
            return numberFormat.format(src);
        }
        else
        {
            return ((Number) src).toString();
        }
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }

}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DataType;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.NumberUtils;

import java.text.NumberFormat;

/**
 * <code>StringToNumber</code> converts a String to a Number. A NumberFormat is used
 * if one is provided.
 */
public class StringToNumber extends AbstractTransformer implements DiscoverableTransformer
{

    private NumberFormat numberFormat;

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToNumber()
    {
        registerSourceType(new SimpleDataType<Object>(String.class));
        setReturnDataType(DataTypeFactory.create(Number.class));
    }

    public StringToNumber(NumberFormat numberFormat)
    {
        this();
        this.numberFormat = numberFormat;
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        if (src == null)
        {
            return null;
        }
        else
        {
            if (numberFormat != null)
            {
                try
                {
                    return NumberUtils.convertNumberToTargetClass(numberFormat.parse((String) src),
                        getReturnClass());
                }
                catch (Exception e)
                {
                    throw new TransformerException(this, e);
                }
            }
            else
            {
                return NumberUtils.parseNumber((String) src, getReturnClass());
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Class<? extends Number> getReturnClass()
    {
        return (Class<Number>) super.getReturnDataType().getType();
    }

    @Override
    public void setReturnDataType(DataType<?> type)
    {
        if (!Number.class.isAssignableFrom(type.getType()))
        {
            throw new IllegalArgumentException("This transformer only supports Number return types.");
        }
        else
        {
            super.setReturnDataType(type);
        }
    }

    @Override
    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    @Override
    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }

}

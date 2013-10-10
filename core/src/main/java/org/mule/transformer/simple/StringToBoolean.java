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
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object
 * representation
 */
public class StringToBoolean extends AbstractTransformer implements DiscoverableTransformer
{

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToBoolean()
    {
        registerSourceType(new SimpleDataType<Object>(String.class));
        setReturnDataType(DataTypeFactory.create(Boolean.class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            if (isAllowNullReturn())
            {
                return null;
            }
            else
            {
                throw new TransformerException(
                    CoreMessages.createStaticMessage("Unable to transform null to a primitive"));
            }
        }
        else
        {
            return Boolean.valueOf((String) src);
        }
    }

    @Override
    public void setReturnDataType(DataType<?> type)
    {
        if (!Boolean.class.isAssignableFrom(type.getType()))
        {
            throw new IllegalArgumentException("This transformer only supports Boolean return types.");
        }
        else
        {
            super.setReturnDataType(type);
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

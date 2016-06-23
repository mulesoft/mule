/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.transformer.AbstractTransformer;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object
 * representation
 */
public class StringToBoolean extends AbstractTransformer implements DiscoverableTransformer
{

    /**
     * Give core transformers a slightly higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToBoolean()
    {
        registerSourceType(DataType.STRING);
        setReturnDataType(DataType.BOOLEAN);
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

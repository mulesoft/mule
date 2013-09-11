/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Resolves data type conversion finding an appropriate converter that is able
 * to execute the required transformation. The lookup is executed dynamically
 * using the discovering of transformers using the application's
 * {@link MuleContext}
 */
public class DynamicDataTypeConversionResolver implements DataTypeConversionResolver
{

    private static final Log logger = LogFactory.getLog(DynamicDataTypeConversionResolver.class);

    private final  MuleContext muleContext;

    public DynamicDataTypeConversionResolver(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Transformer resolve(DataType<?> sourceType, List<DataType<?>> targetDataTypes)
    {
        Transformer transformer = null;

        for (DataType targetDataType : targetDataTypes)
        {
            try
            {
                transformer = muleContext.getRegistry().lookupTransformer(sourceType, targetDataType);

                if (transformer != null)
                {
                    break;
                }
            }
            catch (TransformerException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Unable to find an implicit conversion from " + sourceType + " to " + targetDataType);
                }
            }
        }

        return transformer;
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

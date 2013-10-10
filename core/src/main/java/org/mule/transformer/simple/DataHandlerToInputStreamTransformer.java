/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.IOException;

import javax.activation.DataHandler;

public class DataHandlerToInputStreamTransformer extends AbstractDiscoverableTransformer
{

    public DataHandlerToInputStreamTransformer()
    {
        registerSourceType(DataTypeFactory.create(DataHandler.class));
        setReturnDataType(DataTypeFactory.INPUT_STREAM);
    }

    @Override
    public Object doTransform(Object src, String enc) throws TransformerException
    {
        try
        {
            return ((DataHandler) src).getInputStream();
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
    }
}

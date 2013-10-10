/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

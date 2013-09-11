/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.hello;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

/**
 * <code>ExceptionToString</code> converts an exception to a String,
 * returning the exception's <code>getMessage()</code> result.
 */
public class ExceptionToString extends AbstractTransformer
{

    public ExceptionToString()
    {
        super();
        this.registerSourceType(DataTypeFactory.create(Exception.class));
        this.setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        return ((Exception) src).getMessage();
    }

}

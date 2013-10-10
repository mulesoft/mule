/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

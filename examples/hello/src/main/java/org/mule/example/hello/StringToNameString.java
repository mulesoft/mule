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
 * <code>StringToNameString</code> converts from a String to a NameString object.
 */
public class StringToNameString extends AbstractTransformer
{
    public StringToNameString()
    {
        super();
        this.registerSourceType(DataTypeFactory.STRING);
        this.setReturnDataType(DataTypeFactory.create(NameString.class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        return new NameString((String) src);
    }
}

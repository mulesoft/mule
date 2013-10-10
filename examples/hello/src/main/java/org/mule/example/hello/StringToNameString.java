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
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return new NameString((String) src);
    }

}

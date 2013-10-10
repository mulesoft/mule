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
 * The transformation removes break-lines and newlines from the string, which
 * potentially could have been added during a <code>stdin</code> input operation.
 */
public class StdinToNameString extends AbstractTransformer
{
    public StdinToNameString()
    {
        super();
        this.registerSourceType(DataTypeFactory.STRING);
        this.setReturnDataType(DataTypeFactory.create(NameString.class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        NameString nameString = new NameString();
        String name = (String) src;
        nameString.setName(name.replaceAll("\r", "").replaceAll("\n", "").trim());
        return nameString;
    }
}

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
 * <code>NameStringToChatString</code> is a dummy transformer used in the hello world
 * application to transform the ChatString object into a string.
 */
public class ChatStringToString extends AbstractTransformer
{
    public ChatStringToString()
    {
        super();
        this.registerSourceType(DataTypeFactory.create(ChatString.class));
        this.setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        ChatString chatString = (ChatString)src;
        return chatString.toString();
    }
}

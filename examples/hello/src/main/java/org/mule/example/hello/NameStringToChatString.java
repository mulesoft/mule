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
 * <code>NameStringToChatString</code> converts from a NameString object to a
 * ChatString object.
 */
public class NameStringToChatString extends AbstractTransformer
{
    public NameStringToChatString()
    {
        super();
        this.registerSourceType(DataTypeFactory.create(NameString.class));
        this.setReturnDataType(DataTypeFactory.create(ChatString.class));
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        ChatString chatString = new ChatString();
        NameString nameString = (NameString) src;
        chatString.append(nameString.getGreeting());
        chatString.append(nameString.getName());
        return chatString;
    }
}

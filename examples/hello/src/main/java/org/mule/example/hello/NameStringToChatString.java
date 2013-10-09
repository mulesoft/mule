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
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        ChatString chatString = new ChatString();
        NameString nameString = (NameString) src;
        chatString.append(nameString.getGreeting());
        chatString.append(nameString.getName());
        return chatString;
    }

}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        this.registerSourceType(ChatString.class);
        this.setReturnDataType(DataTypeFactory.create(String.class));
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        ChatString chatString = (ChatString)src;
        return chatString.toString();
    }
}

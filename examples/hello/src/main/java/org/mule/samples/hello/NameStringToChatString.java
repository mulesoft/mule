/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.hello;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>NameStringToChatString</code> converts from a NameString object to a
 * ChatString object.
 */
public class NameStringToChatString extends AbstractTransformer
{

    public NameStringToChatString()
    {
        super();
        this.registerSourceType(NameString.class);
        this.setReturnClass(ChatString.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        ChatString chatString = new ChatString();
        NameString nameString = (NameString) src;
        chatString.append(nameString.getGreeting());
        chatString.append(nameString.getName());
        return chatString;
    }

}

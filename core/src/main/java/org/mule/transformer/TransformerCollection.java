/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** TODO */
public class TransformerCollection extends AbstractMessageAwareTransformer
{
    private List transformers;

    public TransformerCollection(List transformers)
    {
        if (transformers.size() < 1)
        {
            throw new IllegalArgumentException("You must set at least one transformer");
        }
        this.transformers = transformers;
    }

    public TransformerCollection(Transformer[] transformers)
    {
        if (transformers.length < 1)
        {
            throw new IllegalArgumentException("You must set at least one transformer");
        }
        this.transformers = Arrays.asList(transformers);
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        MuleMessage result = message;
        Object temp = message;
        Transformer lastTransformer = null;
        for (Iterator iterator = transformers.iterator(); iterator.hasNext();)
        {
            lastTransformer = (Transformer) iterator.next();
            temp = lastTransformer.transform(temp);
            if (temp instanceof MuleMessage)
            {
                result = (MuleMessage) temp;
            }
            else
            {
                result.setPayload(temp);
            }
        }
        if (lastTransformer != null && lastTransformer.getReturnClass().equals(MuleMessage.class))
        {
            return result;
        }
        else
        {
            return result.getPayload();
        }
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.util.StringUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A referencable chain of transformers that can be used as a single transformer
 */
public class TransformerChain extends AbstractMessageTransformer
{
    private List<Transformer> transformers;

    public TransformerChain(List<Transformer> transformers)
    {
        super();
        if (transformers.size() < 1)
        {
            throw new IllegalArgumentException("You must set at least one transformer");
        }
        this.transformers = new LinkedList<Transformer>(transformers);
    }

    public TransformerChain(Transformer... transformers)
    {
        this(Arrays.asList(transformers));
        this.name = generateTransformerName();
    }

    public TransformerChain(String name, List<Transformer> transformers)
    {
        this(transformers);
        this.name = name;
    }

    public TransformerChain(String name, Transformer... transformers)
    {
        this(name, Arrays.asList(transformers));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
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

    @Override
    public void initialise() throws InitialisationException
    {
        for (Transformer transformer : transformers)
        {
            transformer.initialise();
        }
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        super.setMuleContext(muleContext);
        for (Transformer transformer : transformers)
        {
            transformer.setMuleContext(muleContext);
        }
    }

    @Override
    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        super.setEndpoint(endpoint);
        for (Transformer transformer : transformers)
        {
            transformer.setEndpoint(endpoint);
        }
    }

    @Override
    protected String generateTransformerName()
    {
        String name = transformers.get(0).getClass().getSimpleName();
        int i = name.indexOf("To");
        DataType dt = transformers.get(transformers.size() -1).getReturnDataType();
        if (i > 0 && dt != null)
        {
            String target = dt.getType().getSimpleName();
            if (target.equals("byte[]"))
            {
                target = "byteArray";
            }
            name = name.substring(0, i + 2) + StringUtils.capitalize(target);
        }
        return name;
    }

}

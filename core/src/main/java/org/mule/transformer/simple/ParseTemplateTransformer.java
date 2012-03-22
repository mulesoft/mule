/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.AttributeEvaluator;
import org.mule.util.IOUtils;

import java.io.IOException;

/**
 * Loads a template and parses its content to resolve expressions. The order in which attempts to load the resource is
 * the following: from the file system, from a URL or from the classpath.
 */
public class ParseTemplateTransformer extends AbstractMessageTransformer
{
    private AttributeEvaluator locationEvaluator;

    public ParseTemplateTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        locationEvaluator.initialize(muleContext.getExpressionManager());
    }


    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Object locationValue = locationEvaluator.resolveValue(message);
        if(locationValue == null)
        {
            throw new IllegalArgumentException("Location cannot be null");
        }
        String location = locationValue.toString();

        try
        {
            String template = IOUtils.getResourceAsString(location, this.getClass());
            return muleContext.getExpressionManager().parse(template, message);
        }
        catch(IOException e)
        {
            throw new TransformerException(this, e);
        }
    }

    public void setLocation(String location)
    {
        if(location == null)
        {
            throw new IllegalArgumentException("Location cannot be null");
        }
        locationEvaluator = new AttributeEvaluator(location);
    }
}

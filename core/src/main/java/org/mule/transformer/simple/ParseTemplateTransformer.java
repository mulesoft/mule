/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.util.IOUtils;

/**
 * Loads a template and parses its content to resolve expressions. The order in which attempts to load the resource is
 * the following: from the file system, from a URL or from the classpath.
 */
public class ParseTemplateTransformer extends AbstractMessageTransformer
{
    private String location;
    private String template;

    public ParseTemplateTransformer()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        loadTemplate();
    }

    private void loadTemplate() throws InitialisationException
    {
        try
        {
            if(location == null)
            {
                throw new IllegalArgumentException("Location cannot be null");
            }
            template = IOUtils.getResourceAsString(location, this.getClass());

        }
        catch(Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }


    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        if(template == null)
        {
            throw new IllegalArgumentException("Template cannot be null");
        }

        return muleContext.getExpressionManager().parse(template, message);
    }

    public void setLocation(String location)
    {
        this.location = location;
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.transformer;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.IOUtils;

import java.io.InputStream;

/**
 * Throws an exception if the message does not contain "success".
 */
public class ValidateResponse extends AbstractTransformer
{
    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        String response = null;
        if (src instanceof String)
        {
            response = (String) src;
        }
        else if (src instanceof InputStream)
        {
            response = IOUtils.toString((InputStream) src);
        }
        
        if (response != null && response.contains("success"))
        {
            return response;
        }
        else
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Invalid response from service: " + response));
        }
    }
}



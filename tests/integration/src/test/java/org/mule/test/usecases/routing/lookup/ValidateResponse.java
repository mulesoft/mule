/*
 * $Id$
 * --------------------------------------------------------------------------------------
 *
 * (c) 2003-2008 MuleSource, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSource's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSource. If such an agreement is not in place, you may not use the software.
 */

package org.mule.test.usecases.routing.lookup;

import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.IOUtils;

import java.io.InputStream;

/**
 * Throws an exception if the message does not contain "success".  In the real world, we might use XPath to 
 * extract a particular tag or error code based on the expected response message format.
 */
public class ValidateResponse extends AbstractTransformer
{
    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        String response = null;
        if (src instanceof InputStream)
        {
            response = IOUtils.toString((InputStream) src);
        }
        else if (src instanceof String)
        {
            response = (String) src;
        }
        
        if (response != null && response.contains("<ErrorStatus>Success</ErrorStatus>"))
        {
            return response;
        }
        else
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Invalid response from service: " + response));
        }
    }
}



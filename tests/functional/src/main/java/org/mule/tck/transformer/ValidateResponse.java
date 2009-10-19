/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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



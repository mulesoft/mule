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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HttpRequestToNameString extends AbstractTransformer
{
    private static final String NAME_REQUEST_PARAMETER = "name=";
    
    public HttpRequestToNameString()
    {
        super();
        this.registerSourceType(String.class);
        this.registerSourceType(byte[].class);
        this.setReturnClass(NameString.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        return new NameString(extractNameValue(extractRequestQuery(convertRequestToString(src, encoding))));
        
    }
    
    private String convertRequestToString(Object src, String encoding)
    {
        String srcAsString = null;
        
        if (src instanceof byte[])
        {
            if (encoding != null)
            {
                try
                {
                    srcAsString = new String((byte[])src, encoding);
                }
                catch (UnsupportedEncodingException ex)
                {
                    srcAsString = new String((byte[])src);
                }
            }
            else
            {
                srcAsString = new String((byte[])src);
            }
        }
        else
        {
            srcAsString = src.toString();
        }        
        
        return srcAsString;
    }
    
    private String extractRequestQuery(String request)
    {
        String requestQuery = null;
        
        if (request != null && request.length() > 0 && request.indexOf('?') != -1)
        {
            requestQuery = request.substring(request.indexOf('?') + 1).trim();
        }

        return requestQuery;
    }
    
    private String extractNameValue(String requestQuery) throws TransformerException
    {
        String nameValue = null;
        
        if (requestQuery != null && requestQuery.length() > 0)
        {
            int nameParameterPos = requestQuery.indexOf(NAME_REQUEST_PARAMETER);
            if (nameParameterPos != -1)
            {
                int nextParameterValuePos = requestQuery.indexOf('&'); 
                if (nextParameterValuePos == -1 || nextParameterValuePos < nameParameterPos)
                {
                    nextParameterValuePos = requestQuery.length();
                }

                nameValue = requestQuery.substring(nameParameterPos + NAME_REQUEST_PARAMETER.length(), nextParameterValuePos);
            }
            
            if (nameValue != null && nameValue.length() > 0)
            {
                try
                {
                    nameValue = URLDecoder.decode(nameValue, "UTF-8");
                }
                catch (UnsupportedEncodingException uee)
                {
                    logger.error(uee.getMessage());
                }
            }
        }

        if (nameValue == null)
        {
            nameValue = "";
        }
        
        return nameValue;
    }
}

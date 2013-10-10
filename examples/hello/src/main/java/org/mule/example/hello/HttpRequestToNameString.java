/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.hello;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HttpRequestToNameString extends AbstractTransformer
{
    private static final String NAME_REQUEST_PARAMETER = "name=";
    
    public HttpRequestToNameString()
    {
        super();
        this.registerSourceType(DataTypeFactory.STRING);
        this.registerSourceType(DataTypeFactory.BYTE_ARRAY);
        this.registerSourceType(DataTypeFactory.INPUT_STREAM);
        this.setReturnDataType(DataTypeFactory.create(NameString.class));
    }

    @Override
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
        else if (src instanceof InputStream)
        {
            InputStream input = (InputStream) src;
            try
            {
                srcAsString = IOUtils.toString(input);
            }
            finally
            {
                IOUtils.closeQuietly(input);
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

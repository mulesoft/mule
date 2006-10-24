/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.transformers;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.mule.impl.MuleMessage;
import org.mule.providers.http.HttpConstants;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>HttpClientMethodResponseToObject</code> transforms a http client response
 * to a MuleMessage.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class HttpClientMethodResponseToObject extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7699394620081967116L;

    public HttpClientMethodResponseToObject()
    {
        registerSourceType(HttpMethod.class);
        setReturnClass(UMOMessage.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        Object msg;
        HttpMethod httpMethod = (HttpMethod)src;
        Header contentType = httpMethod.getResponseHeader(HttpConstants.HEADER_CONTENT_TYPE);
        try
        {
            if (contentType != null && !contentType.getValue().startsWith("text/"))
            {
                // TODO properly do streaming
                msg = IOUtils.toByteArray(httpMethod.getResponseBodyAsStream());
            }
            else
            {
                msg = httpMethod.getResponseBodyAsString();
            }
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
        // Standard headers
        Map headerProps = new HashMap();
        Header[] headers = httpMethod.getRequestHeaders();
        String name;
        for (int i = 0; i < headers.length; i++)
        {
            name = headers[i].getName();
            if (name.startsWith(HttpConstants.X_PROPERTY_PREFIX))
            {
                name = name.substring(2);
            }
            headerProps.put(name, headers[i].getValue());
        }
        // Set Mule Properties

        return new MuleMessage(msg, headerProps);
    }
}

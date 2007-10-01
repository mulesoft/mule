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

import java.io.UnsupportedEncodingException;

import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>NameStringToChatString</code> This is test class only for use with the
 * Hello world test application.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpRequestToString extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6438813035354275131L;

    public HttpRequestToString()
    {
        super();
        this.registerSourceType(String.class);
        this.registerSourceType(byte[].class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformers.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        String param = null;
        if (src instanceof byte[])
        {
            if (encoding != null)
            {
                try
                {
                    param = new String((byte[])src, encoding);
                }
                catch (UnsupportedEncodingException ex)
                {
                    param = new String((byte[])src);
                }
            }
            else
            {
                param = new String((byte[])src);
            }
        }
        else
        {
            param = src.toString();
        }
        int equals = param.indexOf("=");
        if (equals > -1)
        {
            return param.substring(equals + 1);
        }
        else
        {
            throw new TransformerException(Message.createStaticMessage("Failed to parse param string: "
                                                                       + param), this);
        }
    }
}

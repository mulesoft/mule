/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.providers.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.NameValuePair;
import org.mule.MuleManager;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

/**
 * <code>HttpMessageAdapter</code> Wraps an incoming Http Request making the
 * payload and heads available a standard message adapter
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HttpMessageAdapter extends AbstractMessageAdapter
 {
    private Object message = null;
    private UMOTransformer trans = new SerializableToByteArray();

    private boolean http11 = true;

    public HttpMessageAdapter(Object message) throws MessagingException {
        if (message instanceof Object[]) {
            this.message = ((Object[]) message)[0];
            if (((Object[]) message).length > 1) {
                properties = (Map) ((Object[]) message)[1];
            }
        } else if (message instanceof byte[]) {
            this.message = (byte[]) message;
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
        String temp = (String) properties.get(HttpConnector.HTTP_VERSION_PROPERTY);
        if (HttpConstants.HTTP10.equalsIgnoreCase(temp)) {
            http11 = false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload() {
        return message;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        if(message instanceof byte[]) {
            return (byte[]) message;
        } else if (message instanceof String) {
            return message.toString().getBytes();
        } else {
            return (byte[]) trans.transform(message);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.providers.UMOMessageAdapter#getPayloadAsString(String encoding)
     */
    public String getPayloadAsString(String encoding) throws Exception {
        if(message instanceof byte[]) {
        	if (encoding != null){
                return new String((byte[])message, encoding);
        	} else {
        	  return new String((byte[])message);
        	}
        } else {
            return message.toString();
        }
    }

    /*
    * (non-Javadoc)
    *
    * @see org.mule.providers.UMOMessageAdapter#getProperty(java.lang.Object)
    */
    public Object getProperty(Object key) {
        if (HttpConstants.HEADER_KEEP_ALIVE.equals(key) || HttpConstants.HEADER_CONNECTION.equals(key)) {
            if (!http11) {
                String connection = (String) super.getProperty(HttpConstants.HEADER_CONNECTION);
                if (connection != null && connection.equalsIgnoreCase("close")) {
                    return "false";
                } else {
                    return "true";
                }
            } else {
                return (super.getProperty(HttpConstants.HEADER_CONNECTION) != null ? "true" : "false");
            }
        } else {
            return super.getProperty(key);
        }
    }

    public String getEncoding() {
        String charset = null;
        Header contenttype = getHeader(HttpConstants.HEADER_CONTENT_TYPE);
        if (contenttype != null) {
            HeaderElement values[] = contenttype.getElements();
            if (values.length == 1) {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null) {
                    charset = param.getValue();
                }
            }
        }
        if (charset != null) {
            return charset;
        } else {
            return MuleManager.getConfiguration().getEncoding();
        }
    }

    public Header getHeader(String name) {
        String value = (String)getProperty(name);
        if(value==null) return null;
        return new Header(name, value);
    }
}

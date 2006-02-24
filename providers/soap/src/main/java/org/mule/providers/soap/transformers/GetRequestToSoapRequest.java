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
 */
package org.mule.providers.soap.transformers;

import org.mule.config.MuleProperties;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesHelper;
import org.mule.util.StringMessageHelper;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A simple transformer for converting an Http GET request into a SOAP request. Usually, you would POST a soap document, but this
 * Transformer can be useful for making simple soap requests
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GetRequestToSoapRequest extends AbstractTransformer {

    public static final String SOAP_HEADER = "<?xml version=\"1.0\" encoding=\"{0}\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><soap:Body>";
    public static final String SOAP_FOOTER = "</soap:Body></soap:Envelope>";
    public static final String DEFAULT_NAMESPACE = "http://www.muleumo.org/soap";

    public GetRequestToSoapRequest() {
        registerSourceType(String.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException {
        String request = src.toString();
        StringBuffer result = new StringBuffer();
        int i = request.indexOf("?");
        if(i > -1) {
            String query = request.substring(i+1);
            Properties p = PropertiesHelper.getPropertiesFromQueryString(query);
            String header = StringMessageHelper.getFormattedMessage(SOAP_HEADER, new Object[]{encoding});
            result.append(header);
            String method = (String)p.remove(MuleProperties.MULE_METHOD_PROPERTY);
            if(method==null) {
                throw new TransformerException(new Message(Messages.PROPERTIES_X_NOT_SET, MuleProperties.MULE_METHOD_PROPERTY), this);
            }
            result.append("<").append(method).append(" xmlns=\"").append(DEFAULT_NAMESPACE).append("\">");
            for (Iterator iterator = p.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry)iterator.next();
                result.append("<").append(entry.getKey()).append(">");
                result.append(entry.getValue());
                result.append("</").append(entry.getKey()).append(">");
            }
            result.append("</").append(method).append(">");
            result.append(SOAP_FOOTER);
        } else {
            throw new TransformerException(new Message(Messages.PROPERTIES_X_NOT_SET, MuleProperties.MULE_METHOD_PROPERTY), this);
        }

        return result.toString();
    }
}

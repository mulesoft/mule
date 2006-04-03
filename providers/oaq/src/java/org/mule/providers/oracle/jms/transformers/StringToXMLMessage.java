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
package org.mule.providers.oracle.jms.transformers;

import oracle.jms.AQjmsSession;
import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

import org.mule.config.i18n.Message;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOException;
import org.mule.umo.transformer.TransformerException;

import javax.jms.Session;

/**
 * Transformer for use with the Oracle Jms Connector.
 * Expects a string containing properly-formed XML.
 * Creates a JMS message whose payload is Oracle's native XML data type.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @see XMLMessageToString
 * @see OracleJmsConnector
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96620/toc.htm">Oracle9i XML Database Developer's Guide - Oracle XML DB</a>
 */
public class StringToXMLMessage extends AbstractTransformer {

    public StringToXMLMessage() {
        super();
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(AdtMessage.class);
    }

    /**
     * @param xml - String containing properly-formed XML.
     * @return JMS message whose payload is Oracle's native XML data type
     */
    public Object doTransform(Object xml, String encoding) throws TransformerException {
        Session session = null;
        AdtMessage message = null;
        XMLType xmltype = null;

        // Get the (already open) OracleAQ session.
        try { session = (Session) getEndpoint().getConnector().getDispatcher(getEndpoint()).getDelegateSession();
        } catch (UMOException e) { throw new TransformerException(this, e); }
        if (!(session instanceof AQjmsSession)) {
            throw new TransformerException(Message.createStaticMessage("Endpoint must be an OracleAQ session."), this);
        }

        try {
            // Make sure the object to transform is one of the supported types for this
            // transformer.
            if (xml instanceof String) {
                logger.debug("Converting string to XMLType: " + xml);
                xmltype = XMLType.createXML(((AQjmsSession) session).getDBConnection(),
                                              (String) xml);
            } else if (xml instanceof byte[]) {
                logger.debug("Converting bytes to XMLType: " + xml);
                xmltype = XMLType.createXML(((AQjmsSession) session).getDBConnection(),
                                              new String((byte[]) xml, encoding));
            } else {
                throw new TransformerException(Message.createStaticMessage("Object to transform is not one of the supported types for this transformer."), this);
            }

            // Create the JMS message.
            message = ((AQjmsSession) session).createAdtMessage();
            message.setAdtPayload(xmltype);

        } catch (Exception e) {
            throw new TransformerException(this, e);
        }
        return message;
    }
}
/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms.transformers;

import org.mule.config.i18n.Message;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringMessageUtils;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

import javax.jms.JMSException;
import javax.jms.Session;

import oracle.jms.AQjmsSession;
import oracle.jms.AdtMessage;
import oracle.sql.CLOB;
import oracle.xdb.XMLType;

/**
 * Transformer for use with the Oracle Jms Connector. Expects a string containing
 * properly-formed XML. Creates a JMS message whose payload is Oracle's native XML
 * data type.
 * 
 * @see OracleJmsConnector
 * @see <a href="http://otn.oracle.com/pls/db102/">XML DB Developer's Guide</a>
 */
public class StringToXMLMessage extends AbstractEventAwareTransformer
{
    public StringToXMLMessage()
    {
        super();
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(AdtMessage.class);
    }

    /**
     * @param src - String or byte[] containing properly-formed XML.
     * @return JMS message whose payload is Oracle's native XML data type
     */
    public Object transform(Object src, String encoding, UMOEventContext context) throws TransformerException
    {
        Session session;
        AdtMessage message;
        XMLType xmltype;

        try
        {
            // Get the Oracle AQ session for this event.
            if (endpoint.getTransactionConfig().isTransacted() == false)
            {
                // The tranformation _must_ occur within the same session used previously by the 
                // dispatcher, otherwise we get "JMS-204: An error occurred in the AQ JNI layer"
                // Using a transacted endpoint should ensure that the same session is used.
                throw new TransformerException(Message.createStaticMessage("This transformer may only be used with a transacted endpoint.  Refer to http://mule.codehaus.org/display/MULE/Transaction+Management for more information."), this);
            }
            session = ((JmsConnector) endpoint.getConnector()).getSessionFromTransaction();
            if (session == null) {
                throw new TransformerException(Message.createStaticMessage("No JMS session associated with this endpoint."), this);
            }
            if ((session instanceof AQjmsSession) == false) {
                throw new TransformerException(Message.createStaticMessage("Endpoint must be an OracleAQ session."), this);
            }

            // Prepare the XML string.
            String xml;
            if (src instanceof byte[]) {
                xml = new String((byte[]) src, encoding);
            }
            else if (src instanceof String) {
                xml = (String) src;
            }
            else throw new TransformerException(Message.createStaticMessage("Object to transform is not one of the supported types for this transformer."), this);

            logger.debug("Creating an Oracle XMLType based on the following XML:\n" + StringMessageUtils.truncate(xml, 200, false));

            // Create a temporary CLOB and pass this to the XMLType.createXML() factory.
            // Note: if we pass the xml string directly to XMLType.createXML() as a
            // parameter, the character set is not preserved properly (probably a bug
            // in the Oracle library).
            CLOB clob = CLOB.createTemporary(((AQjmsSession) session).getDBConnection(), true, CLOB.DURATION_SESSION);
            try {
                Writer clobStream = clob.getCharacterOutputStream();
                try {
                    clobStream.write(xml);
                } finally {
                    clobStream.close();
                }
                xmltype = XMLType.createXML(((AQjmsSession) session).getDBConnection(), clob);

                // Create the JMS message.
                message = ((AQjmsSession) session).createAdtMessage();
                message.setAdtPayload(xmltype);
                return message;
            } finally {
                // TODO Need to put this somewhere but apparently not here...
                //clob.freeTemporary();
            }
        }
        catch (JMSException e) { throw new TransformerException(this, e); }
        catch (SQLException e) { throw new TransformerException(this, e); }
        catch (IOException e) { throw new TransformerException(this, e); }
    }
}

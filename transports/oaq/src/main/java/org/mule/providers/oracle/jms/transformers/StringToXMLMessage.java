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

import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

import org.mule.config.i18n.Message;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

import javax.jms.Session;

/**
 * Transformer for use with the Oracle Jms Connector. Expects a string containing
 * properly-formed XML. Creates a JMS message whose payload is Oracle's native XML
 * data type.
 * 
 * @see XMLMessageToString
 * @see OracleJmsConnector
 * @see <a href="http://otn.oracle.com/pls/db102/">XML DB Developer's Guide</a>
 */
public class StringToXMLMessage extends AbstractEventAwareTransformer
{

    private static final long serialVersionUID = 8476470235704172556L;

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
        Session session = null;
        AdtMessage message = null;
        XMLType xmltype = null;

        throw new TransformerException(
            Message.createStaticMessage("This transformer is currently unsupported until issue MULE-1079 is resolved."),
            this);
        /*
         * try { // Get the Oracle AQ session for this event. // TODO This property
         * is no longer set by JmsMessageDispatcher, see MULE-1079 session =
         * (Session)
         * context.getMessage().getProperty(MuleProperties.MULE_JMS_SESSION); if
         * (session == null) { throw new
         * TransformerException(Message.createStaticMessage("The current JMS session
         * should have been stored as a property for this event."), this); } if
         * ((session instanceof AQjmsSession) == false) { throw new
         * TransformerException(Message.createStaticMessage("Endpoint must be an
         * OracleAQ session."), this); } // Prepare the XML string. String xml; if
         * (src instanceof byte[]) { xml = new String((byte[]) src, encoding); } else
         * if (src instanceof String) { xml = (String) src; } else throw new
         * TransformerException(Message.createStaticMessage("Object to transform is
         * not one of the supported types for this transformer."), this);
         * logger.debug("Creating an Oracle XMLType based on the following XML:\n" +
         * StringMessageUtils.truncate(xml, 200, false)); // Create a temporary CLOB
         * and pass this to the XMLType.createXML() factory. // Note: if we pass the
         * xml string directly to XMLType.createXML() as a // parameter, the
         * character set is not preserved properly (probably a bug // in the Oracle
         * library). CLOB clob = CLOB.createTemporary(((AQjmsSession)
         * session).getDBConnection(), true, CLOB.DURATION_SESSION); try { Writer
         * clobStream = clob.getCharacterOutputStream(); try { clobStream.write(xml); }
         * finally { clobStream.close(); } xmltype =
         * XMLType.createXML(((AQjmsSession) session).getDBConnection(), clob); //
         * Create the JMS message. message = ((AQjmsSession)
         * session).createAdtMessage(); message.setAdtPayload(xmltype); return
         * message; } finally { // TODO Need to put this somewhere but apparently not
         * here... //clob.freeTemporary(); } } catch (JMSException e) { throw new
         * TransformerException(this, e); } catch (SQLException e) { throw new
         * TransformerException(this, e); } catch (IOException e) { throw new
         * TransformerException(this, e); }
         */
    }
}

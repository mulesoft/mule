/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.SessionHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassSpecificObjectInputStream;
import org.mule.util.ObjectInputStreamProvider;
import org.mule.util.SerializationUtils;

import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.util.Collections.unmodifiableSet;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A session handler used to store and retrieve session information on an
 * event. The MuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as an array of bytes (byte[])
 */
public class SerializeOnlySessionHandler implements SessionHandler
{
    public static final String ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY = SYSTEM_PROPERTY_PREFIX
            + "session.serialization.native.enable";

    public static final String ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP_PROPERTY = SYSTEM_PROPERTY_PREFIX
            + "session.serialization.http.fallback.maxTimestamp";
    public static final String ALLOW_MSG_FALLBACK_MAX_TIMESTAMP_PROPERTY = SYSTEM_PROPERTY_PREFIX
            + "session.serialization.msg.fallback.maxTimestamp";

    protected boolean ACTIVATE_NATIVE_SESSION_SERIALIZATION = Boolean
            .getBoolean(ACTIVATE_NATIVE_SESSION_SERIALIZATION_PROPERTY);

    protected static final Long ENFORCE_SERIALIZATION_SINCE_TIMESTAMP;

    protected static final Long ALLOW_MSG_FALLBACK_MAX_TIMESTAMP;
    protected static final Long ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP;

    protected static final Set<String> MESSAGING_TRANSPORTS;
    protected static final Set<String> HTTP_TRANSPORTS;

    protected static Log logger = LogFactory.getLog(SerializeOnlySessionHandler.class);

    static
    {
//      session.serialization.enforce.sinceTimestamp
//      mule.session.serialization.msg.fallback.maxTimestamp
//      mule.session.serialization.http.fallback.maxTimestamp
//      mule.session.sign.secretKey
//      mule.session.sign.cloudHub.secretKey
//      mule.session.serialization.native.enable

        Set<String> messagingTransports = new HashSet<>();
        messagingTransports.add("vm");
        messagingTransports.add("jms");
        messagingTransports.add("wmq");
        messagingTransports.add("amqp");
        messagingTransports.add("amqps");
        messagingTransports.add("imap");
        messagingTransports.add("imaps");
        messagingTransports.add("pop3");
        messagingTransports.add("pop3s");
        messagingTransports.add("msmq");
        MESSAGING_TRANSPORTS = unmodifiableSet(messagingTransports);

        Set<String> httpTransports = new HashSet<>();
        httpTransports.add("http");
        httpTransports.add("https");
        httpTransports.add("jetty");
        httpTransports.add("jetty-ssl");
        httpTransports.add("axis");
        httpTransports.add("servlet");
        HTTP_TRANSPORTS = unmodifiableSet(httpTransports);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        logger.info("SE-13712 applied. " + "" );

        String sinceTimestamp = getProperty(SYSTEM_PROPERTY_PREFIX + "session.serialization.enforce.sinceTimestamp",
                getProperty(SYSTEM_PROPERTY_PREFIX + "session.sign.serializer.sinceTimestamp"));
        try
        {
            if (sinceTimestamp == null)
            {
                logger.info("    enforce.sinceTimestamp: " + "Always");
                ENFORCE_SERIALIZATION_SINCE_TIMESTAMP = 0L;
            }
            else
            {
                logger.info("    enforce.sinceTimestamp: " + sinceTimestamp);
                ENFORCE_SERIALIZATION_SINCE_TIMESTAMP = dateFormat.parse(sinceTimestamp).getTime();
            }
        }
        catch (ParseException e)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not parse '" + SYSTEM_PROPERTY_PREFIX + "session.sign.serializer.sinceTimestamp' with value '" + sinceTimestamp + "'. Expected format is 'yyyy-MM-dd'T'HH:mm:ss.SSSZ'"), e);
        }

        String msgMaxTimestamp = getProperty(SYSTEM_PROPERTY_PREFIX + "session.serialization.msg.fallback.maxTimestamp",
                getProperty(SYSTEM_PROPERTY_PREFIX + "session.sign.msg.fallback.maxTimestamp"));
        try
        {
            if (msgMaxTimestamp == null)
            {
                logger.info("    msg.fallback.maxTimestamp: " + "Disabled");
                ALLOW_MSG_FALLBACK_MAX_TIMESTAMP = 0L;
            } else
            {
                logger.info("    msg.fallback.maxTimestamp: " + msgMaxTimestamp);
                ALLOW_MSG_FALLBACK_MAX_TIMESTAMP = dateFormat.parse(msgMaxTimestamp).getTime();
            }
        }
        catch (ParseException e)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not parse '" + SYSTEM_PROPERTY_PREFIX + "session.serialization.msg.fallback.maxTimestamp' with value '" + msgMaxTimestamp + "'. Expected format is 'yyyy-MM-dd'T'HH:mm:ss.SSSZ'"), e);
        }

        String httpMaxTimestamp = getProperty(SYSTEM_PROPERTY_PREFIX + "session.serialization.http.fallback.maxTimestamp",
                getProperty(SYSTEM_PROPERTY_PREFIX + "session.sign.http.fallback.maxTimestamp"));
        try
        {
            if (httpMaxTimestamp == null)
            {
                logger.info("    http.fallback.maxTimestamp: " + "Disabled");
                ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP = 0L;
            } else
            {
                logger.info("    http.fallback.maxTimestamp: " + httpMaxTimestamp);
                ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP = dateFormat.parse(httpMaxTimestamp).getTime();
            }
        }
        catch (ParseException e)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Could not parse '" + SYSTEM_PROPERTY_PREFIX + "session.serialization.http.fallback.maxTimestamp' with value '" + httpMaxTimestamp + "'. Expected format is 'yyyy-MM-dd'T'HH:mm:ss.SSSZ'"), e);
        }
    }

    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        MuleSession session = null;
        byte[] serializedSession = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);

        if (serializedSession != null)
        {
            ObjectInputStreamProvider provider = new ClassSpecificObjectInputStream.Provider(MuleSession.class);
            session = (MuleSession) SerializationUtils.deserialize(serializedSession, message.getMuleContext(), provider);
        }
        return session;
    }

    /**
     * @deprecated Use retrieveSessionInfoFromMessage(MuleMessage message) instead
     */
    public void retrieveSessionInfoFromMessage(MuleMessage message, MuleSession session) throws MuleException
    {
        session = retrieveSessionInfoFromMessage(message);
    }

    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {
        byte[] serializedSession = SerializationUtils.serialize(removeNonSerializableProperties(session,message.getMuleContext()));
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding serialized Session header to message: " + serializedSession);
        }
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, serializedSession);
    }
    
    protected boolean isFallbackAllowed(String endpoint)
    {
        for (String messagingTransport : MESSAGING_TRANSPORTS)
        {
            if(endpoint.startsWith(messagingTransport + ":"))
            {
                return currentTimeMillis() < ALLOW_MSG_FALLBACK_MAX_TIMESTAMP;
            }
        }

        // By default use the grace period from the http type protocols, which should be lower
        return currentTimeMillis() < ALLOW_HTTP_FALLBACK_MAX_TIMESTAMP;
    }

    private boolean isNativeSerializationActivated(String endpoint)
    {
        return ACTIVATE_NATIVE_SESSION_SERIALIZATION && currentTimeMillis() > ENFORCE_SERIALIZATION_SINCE_TIMESTAMP;
    }

    protected String getEndpoint(MuleMessage message)
    {
        return valueOf(message.getInboundProperty("MULE_ENDPOINT"));
    }

    protected MuleSession removeNonSerializableProperties(final MuleSession session,
                                                          final MuleContext muleContext)
    {
        DefaultMuleSession copy = new DefaultMuleSession(session);
        copy.removeNonSerializableProperties();
        return copy;
    }
    
    /**
     * @deprecated This method is no longer needed and will be removed in the next major release
     */
    public String getSessionIDKey()
    {
        return "ID";
    }
}

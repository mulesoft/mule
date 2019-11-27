/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.session;

import static java.lang.System.arraycopy;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.StreamCorruptedException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.model.SessionException;
import org.mule.api.serialization.SerializationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.Base64;

/**
 * A session handler used to store and retrieve session information on an
 * event. The DefaultMuleSession information is stored as a header on the message (does not
 * support Tcp, Udp, etc. unless the MuleMessage object is serialised across the
 * wire). The session is stored in the "MULE_SESSION" property as Base64 encoded
 * byte array.
 */
public class SerializeAndEncodeSessionHandler extends SerializeOnlySessionHandler
{

    private static final byte[] SECRET_KEY;
    private static final int SIGNATURE_LENGTH = 32;
    private static final Mac MAC_SIGNER;

    public static final String SESSION_SIGN_SECRET_KEY = SYSTEM_PROPERTY_PREFIX + "session.sign.secretKey";
    public static final String SESSION_SIGN_CH_SECRET_KEY = SYSTEM_PROPERTY_PREFIX + "session.sign.cloudHub.secretKey";
    
    static
    {
        Log logger = LogFactory.getLog(SerializeAndEncodeSessionHandler.class);

        if (System.getProperty(SESSION_SIGN_SECRET_KEY) != null)
        {
            SECRET_KEY = System.getProperty(SESSION_SIGN_SECRET_KEY).getBytes(UTF_8);
        }
        else if(System.getProperty(SESSION_SIGN_CH_SECRET_KEY) != null)
        {
            SECRET_KEY = System.getProperty(SESSION_SIGN_CH_SECRET_KEY).getBytes(UTF_8);
        }
        else
        {
            SECRET_KEY = null;
        }
        
        if (SECRET_KEY != null)
        {
            try
            {
                String algorithm = "HmacSHA256";
                MAC_SIGNER = Mac.getInstance(algorithm);
                SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY, algorithm);
                MAC_SIGNER.init(secretKeySpec);
            }
            catch (NoSuchAlgorithmException | InvalidKeyException e)
            {
                logger.error("Could not init class 'SerializeAndEncodeSessionHandler'", e);
                throw new MuleRuntimeException(e);
            }
        }
        else
        {
            MAC_SIGNER = null;
        }
    }

    @Override
    public MuleSession retrieveSessionInfoFromMessage(MuleMessage message) throws MuleException
    {
        MuleSession session = null;
        String serializedEncodedSession = message.getInboundProperty(MuleProperties.MULE_SESSION_PROPERTY);
        
        if (serializedEncodedSession != null)
        {
            byte[] serializedSession = Base64.decodeWithoutUnzipping(serializedEncodedSession);
            if (serializedSession != null)
            {
                String endpoint = getEndpoint(message);
                boolean signatureOk = false;
                try
                {
                    byte[] signedSerializedSession = getSigned(serializedSession, endpoint);
                    serializedSession = signedSerializedSession;
                    signatureOk = true;
                }
                catch (SessionSignatureException e)
                {
                    logger.warn("Session could not be deserialized: " + e.getMessage());
                    session = null;
                }

                if(signatureOk && serializedSession != null)
                {
                    try
                    {
                        session = deserialize(message, serializedSession);
                    }
                    catch (SerializationException e)
                    {
                        Throwable rootCause = getRootCause(e);
                        
                        if (rootCause != null && (rootCause instanceof InvalidClassException || rootCause instanceof StreamCorruptedException))
                        {
                            logger.warn("Session could not be deserialized due to class incompatibility: " + e.getCause().getMessage());
                            session = null;
                        }
                        else
                        {
                            throw e;
                        }
                    }
                }

            }
        }
        return session;
    }

    @Override
    public void storeSessionInfoToMessage(MuleSession session, MuleMessage message) throws MuleException
    {        
        if (SECRET_KEY == null && !ACTIVATE_NATIVE_SESSION_SERIALIZATION)
        {
            // Disable session when no config is provided
            session = new DefaultMuleSession();
        }
        else
        {
            session = removeNonSerializableProperties(session, message.getMuleContext());
        }
        byte[] serializedSession = serialize(message, session);
        
        if (SECRET_KEY != null)
        {
            serializedSession = sign(serializedSession);
        }

        String serializedEncodedSession;
        try
        {
            serializedEncodedSession = Base64.encodeBytes(serializedSession, Base64.DONT_BREAK_LINES);
        }
        catch (IOException e)
        {
            throw new SessionException(MessageFactory.createStaticMessage("Unable to serialize MuleSession"), e);
        }
        
        if (logger.isDebugEnabled())
        {
            logger.debug("Adding serialized and base64-encoded Session header to message: " + serializedEncodedSession);
        }
        message.setOutboundProperty(MuleProperties.MULE_SESSION_PROPERTY, serializedEncodedSession);
    }

    private byte[] getSigned(byte[] signedData, String endpoint)
    {
        // If we have no key configured, the session is not processed
        if (SECRET_KEY == null && !ACTIVATE_NATIVE_SESSION_SERIALIZATION)
        {
            // Disable session when no config is provided
            
            if(signedData.length > 0)
            {
                logger.warn("Trying to deserialize a session but no signature validation key specified.");
            }
            return null;
        }

        if(SECRET_KEY != null)
        {
            // validate the placeholder byte to use as a version flag
            if (signedData.length < SIGNATURE_LENGTH + 1 || signedData[0] != 1)
            {
                throw new SessionSignatureException(
                        CoreMessages.createStaticMessage("Serialized session data does not contain a signature!"));
            }

            byte[] calcHmac = new byte[SIGNATURE_LENGTH];
            byte[] data = new byte[signedData.length - SIGNATURE_LENGTH - 1];

            arraycopy(signedData, 1, calcHmac, 0, SIGNATURE_LENGTH);
            arraycopy(signedData, 1 + SIGNATURE_LENGTH, data, 0, signedData.length - SIGNATURE_LENGTH - 1);

            if (!Arrays.equals(calcHmac, calcHmac(SECRET_KEY, data)))
            {
                throw new SessionSignatureException(
                        CoreMessages.createStaticMessage("Signatures do not match for deserializing session!"));
            }

            return data;
        }
        else
        {
            // Keep the data as it is for it to be handled downstream
            return signedData;
        }
    }

    private byte[] sign(byte[] data)
    {
        byte[] calcHmac = calcHmac(SECRET_KEY, data);
        byte[] result = new byte[1 + SIGNATURE_LENGTH + data.length];

        // placeholder byte to use as a version flag in case of needed changes in the future
        result[0] = 1;
        arraycopy(calcHmac, 0, result, 1, SIGNATURE_LENGTH);
        arraycopy(data, 0, result, 1 + SIGNATURE_LENGTH, data.length);
        return result;
    }

    static public synchronized byte[] calcHmac(byte[] secretKey, byte[] message)
    {
        byte[] hmac = null;
        try
        {
            hmac = MAC_SIGNER.doFinal(message);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to calculate hmac", e);
        }
        return hmac;
    }

    private static final class SessionSignatureException extends MuleRuntimeException
    {

        private static final long serialVersionUID = 2605972894599363699L;

        public SessionSignatureException(Message message)
        {
            super(message);
        }

    }
}

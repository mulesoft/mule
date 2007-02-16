/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.wssecurity.headers;

import org.mule.umo.UMOMessage;

import java.util.Properties;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.handler.WSHandlerConstants;

public class WsSecurityHeadersSetter
{
    /**
     * This method searches for ws-security properties in the message received and
     * returns these properties
     * 
     * @param message
     * @return
     */
    public Properties addSecurityHeaders(UMOMessage message)
    {
        Properties props = new Properties();
        props.setProperty(WSHandlerConstants.ACTION, (String)message
            .getProperty(WSHandlerConstants.ACTION));

        String passwordType;
        if (message.getProperty(WSConstants.PW_DIGEST) != null)
        {
            passwordType = WSConstants.PW_DIGEST;
        }
        else
        {
            passwordType = WSConstants.PW_TEXT;
        }
        props.setProperty(WSHandlerConstants.PASSWORD_TYPE, passwordType);

        if (message.getProperty(WSHandlerConstants.USER) != null)
        {
            props.setProperty(WSHandlerConstants.USER, (String)message
                .getProperty(WSHandlerConstants.USER));
        }

        if (message.getProperty(WSHandlerConstants.PW_CALLBACK_CLASS) != null)
        {
            props.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, (String)message
                .getProperty(WSHandlerConstants.PW_CALLBACK_CLASS));
        }

        if (message.getProperty(WSHandlerConstants.PASSWORD_TYPE) != null)
        {
            props.setProperty(WSHandlerConstants.PASSWORD_TYPE, (String)message
                .getProperty(WSHandlerConstants.PASSWORD_TYPE));
        }

        if (message.getProperty(WSHandlerConstants.PW_CALLBACK_REF) != null)
        {
            props.setProperty(WSHandlerConstants.PW_CALLBACK_REF, (String)message
                .getProperty(WSHandlerConstants.PW_CALLBACK_REF));
        }

        if (message.getProperty(WSHandlerConstants.ACTOR) != null)
        {
            props.setProperty(WSHandlerConstants.ACTOR, (String)message
                .getProperty(WSHandlerConstants.ACTOR));
        }
        if (message.getProperty(WSHandlerConstants.MUST_UNDERSTAND) != null)
        {
            props.setProperty(WSHandlerConstants.MUST_UNDERSTAND, (String)message
                .getProperty(WSHandlerConstants.MUST_UNDERSTAND));
        }
        if (message.getProperty(WSHandlerConstants.NO_SECURITY) != null)
        {
            props.setProperty(WSHandlerConstants.NO_SECURITY, (String)message
                .getProperty(WSHandlerConstants.NO_SECURITY));
        }
        if (message.getProperty(WSHandlerConstants.NO_SERIALIZATION) != null)
        {
            props.setProperty(WSHandlerConstants.NO_SERIALIZATION, (String)message
                .getProperty(WSHandlerConstants.NO_SERIALIZATION));
        }
        if (message.getProperty(WSHandlerConstants.ROLE) != null)
        {
            props.setProperty(WSHandlerConstants.ROLE, (String)message
                .getProperty(WSHandlerConstants.ROLE));
        }

        // if username token
        if (message.getProperty(WSHandlerConstants.ACTION)
                .equals(WSHandlerConstants.USERNAME_TOKEN))
        {
            if (message.getProperty(WSHandlerConstants.ADD_UT_ELEMENTS) != null)
            {
                props.setProperty(WSHandlerConstants.ADD_UT_ELEMENTS, (String)message
                    .getProperty(WSHandlerConstants.ADD_UT_ELEMENTS));
            }
        }

        // if timestamp
        if (message.getProperty(WSHandlerConstants.ACTION)
                .equals(WSHandlerConstants.TIMESTAMP))
        {
            if (message.getProperty(WSHandlerConstants.TIMESTAMP_PRECISION) != null)
            {
                props.setProperty(WSHandlerConstants.TIMESTAMP_PRECISION, (String)message
                    .getProperty(WSHandlerConstants.TIMESTAMP_PRECISION));
            }
            if (message.getProperty(WSHandlerConstants.TIMESTAMP_STRICT) != null)
            {
                props.setProperty(WSHandlerConstants.TIMESTAMP_STRICT, (String)message
                    .getProperty(WSHandlerConstants.TIMESTAMP_STRICT));
            }
            if (message.getProperty(WSHandlerConstants.TTL_TIMESTAMP) != null)
            {
                props.setProperty(WSHandlerConstants.TTL_TIMESTAMP, (String)message
                    .getProperty(WSHandlerConstants.TTL_TIMESTAMP));
            }
        }

        // if encrypted
        if (message.getProperty(WSHandlerConstants.ACTION)
                .equals(WSHandlerConstants.ENCRYPT))
        {
            if (message.getProperty(WSHandlerConstants.ENC_PROP_FILE) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_PROP_FILE, (String)message
                    .getProperty(WSHandlerConstants.ENC_PROP_FILE));
            }
            if (message.getProperty(WSHandlerConstants.ENC_CALLBACK_CLASS) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_CALLBACK_CLASS, (String)message
                    .getProperty(WSHandlerConstants.ENC_CALLBACK_CLASS));
            }
            if (message.getProperty(WSHandlerConstants.ENC_CALLBACK_REF) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_CALLBACK_REF, (String)message
                    .getProperty(WSHandlerConstants.ENC_CALLBACK_REF));
            }
            if (message.getProperty(WSHandlerConstants.ENC_KEY_ID) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_KEY_ID, (String)message
                    .getProperty(WSHandlerConstants.ENC_KEY_ID));
            }
            if (message.getProperty(WSHandlerConstants.ENC_KEY_NAME) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_KEY_NAME, (String)message
                    .getProperty(WSHandlerConstants.ENC_KEY_NAME));
            }
            if (message.getProperty(WSHandlerConstants.ENC_KEY_TRANSPORT) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_KEY_TRANSPORT, (String)message
                    .getProperty(WSHandlerConstants.ENC_KEY_TRANSPORT));
            }
            if (message.getProperty(WSHandlerConstants.ENC_SYM_ALGO) != null)
            {
                props.setProperty(WSHandlerConstants.ENC_SYM_ALGO, (String)message
                    .getProperty(WSHandlerConstants.ENC_SYM_ALGO));
            }
            if (message.getProperty(WSHandlerConstants.ENCRYPTION_PARTS) != null)
            {
                props.setProperty(WSHandlerConstants.ENCRYPTION_PARTS, (String)message
                    .getProperty(WSHandlerConstants.ENCRYPTION_PARTS));
            }

            if (message.getProperty(WSHandlerConstants.ENCRYPTION_USER) != null)
            {
                props.setProperty(WSHandlerConstants.ENCRYPTION_USER, (String)message
                    .getProperty(WSHandlerConstants.ENCRYPTION_USER));
            }

            if (message.getProperty(WSHandlerConstants.DEC_PROP_FILE) != null)
            {
                props.setProperty(WSHandlerConstants.DEC_PROP_FILE, (String)message
                    .getProperty(WSHandlerConstants.DEC_PROP_FILE));
            }
        }

        // Saml
        if (message.getProperty(WSHandlerConstants.SAML_PROP_FILE) != null)
        {
            props.setProperty(WSHandlerConstants.SAML_PROP_FILE, (String)message
                .getProperty(WSHandlerConstants.SAML_PROP_FILE));
        }

        // if signed
        if (message.getProperty(WSHandlerConstants.ACTION)
                .equals(WSHandlerConstants.SIGNATURE)
            || message.getProperty(WSHandlerConstants.ACTION)
                .equals(WSHandlerConstants.SAML_TOKEN_SIGNED))
        {
            if (message.getProperty(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION) != null)
            {
                props.setProperty(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION, (String)message
                    .getProperty(WSHandlerConstants.ENABLE_SIGNATURE_CONFIRMATION));
            }

            if (message.getProperty(WSHandlerConstants.SIG_KEY_ID) != null)
            {
                props.setProperty(WSHandlerConstants.SIG_KEY_ID, (String)message
                    .getProperty(WSHandlerConstants.SIG_KEY_ID));
            }

            if (message.getProperty(WSHandlerConstants.SIG_ALGO) != null)
            {
                props.setProperty(WSHandlerConstants.SIG_ALGO, (String)message
                    .getProperty(WSHandlerConstants.SIG_ALGO));
            }

            if (message.getProperty(WSHandlerConstants.SIG_CONF_DONE) != null)
            {
                props.setProperty(WSHandlerConstants.SIG_CONF_DONE, (String)message
                    .getProperty(WSHandlerConstants.SIG_CONF_DONE));
            }

            if (message.getProperty(WSHandlerConstants.SIG_PROP_FILE) != null)
            {
                props.setProperty(WSHandlerConstants.SIG_PROP_FILE, (String)message
                    .getProperty(WSHandlerConstants.SIG_PROP_FILE));
            }
        }
        return props;
    }
}

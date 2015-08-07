/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.apache.ws.security.WSPasswordCallback.DECRYPT;
import static org.apache.ws.security.handler.WSHandlerConstants.DEC_PROP_REF_ID;
import static org.apache.ws.security.handler.WSHandlerConstants.ENCRYPT;
import static org.mule.module.ws.security.WSCryptoUtils.createKeyStoreProperties;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextKeyStoreConfiguration;

import java.util.Map;
import java.util.Properties;

import org.apache.ws.security.WSPasswordCallback;

/**
 * Decrypts an encrypted SOAP response, using the private key of the key-store in the provided TLS context.
 */
public class WssDecryptSecurityStrategy extends AbstractSecurityStrategy
{

    private static final String WS_DECRYPT_PROPERTIES_KEY = "decryptProperties";

    private TlsContextFactory tlsContextFactory;

    @Override
    public void apply(Map<String, Object> outConfigProperties, Map<String, Object> inConfigProperties)
    {
        final TlsContextKeyStoreConfiguration keyStoreConfig = tlsContextFactory.getKeyStoreConfiguration();

        appendAction(inConfigProperties, ENCRYPT);

        Properties decryptionProperties = createKeyStoreProperties(keyStoreConfig);

        inConfigProperties.put(DEC_PROP_REF_ID, WS_DECRYPT_PROPERTIES_KEY);
        inConfigProperties.put(WS_DECRYPT_PROPERTIES_KEY, decryptionProperties);

        addPasswordCallbackHandler(inConfigProperties, new WSPasswordCallbackHandler(DECRYPT)
        {
            @Override
            public void handle(WSPasswordCallback passwordCallback)
            {
                passwordCallback.setPassword(keyStoreConfig.getKeyPassword());
            }
        });
    }

    public TlsContextFactory getTlsContext()
    {
        return tlsContextFactory;
    }

    public void setTlsContext(TlsContextFactory tlsContextFactory)
    {
        this.tlsContextFactory = tlsContextFactory;
    }

}

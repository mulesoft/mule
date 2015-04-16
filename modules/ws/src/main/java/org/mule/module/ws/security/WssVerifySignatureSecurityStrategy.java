/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.apache.ws.security.components.crypto.Merlin.LOAD_CA_CERTS;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_FILE;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_PASSWORD;
import static org.apache.ws.security.components.crypto.Merlin.TRUSTSTORE_TYPE;
import static org.apache.ws.security.handler.WSHandlerConstants.SIGNATURE;
import static org.apache.ws.security.handler.WSHandlerConstants.SIG_PROP_REF_ID;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextTrustStoreConfiguration;

import java.util.Map;
import java.util.Properties;

import org.apache.ws.security.components.crypto.Merlin;

/**
 * Verifies the signature of a SOAP response, using certificates of the trust-store in the provided TLS context.
 */
public class WssVerifySignatureSecurityStrategy extends AbstractSecurityStrategy
{
    private static final String WS_CRYPTO_PROVIDER_KEY = "org.apache.ws.security.crypto.provider";
    private static final String WS_SIGNATURE_PROPERTIES_KEY = "signatureProperties";

    private TlsContextFactory tlsContextFactory;

    @Override
    public void apply(Map<String, Object> outConfigProperties, Map<String, Object> inConfigProperties)
    {
        appendAction(inConfigProperties, SIGNATURE);

        Properties signatureProperties = new Properties();
        signatureProperties.setProperty(WS_CRYPTO_PROVIDER_KEY, Merlin.class.getCanonicalName());

        if (tlsContextFactory == null)
        {
            signatureProperties.setProperty(LOAD_CA_CERTS, String.valueOf(true));
        }
        else
        {
            TlsContextTrustStoreConfiguration trustStoreConfig = tlsContextFactory.getTrustStoreConfiguration();
            signatureProperties.setProperty(TRUSTSTORE_TYPE, trustStoreConfig.getType());
            signatureProperties.setProperty(TRUSTSTORE_PASSWORD, trustStoreConfig.getPassword());
            signatureProperties.setProperty(TRUSTSTORE_FILE, trustStoreConfig.getPath());
        }

        inConfigProperties.put(SIG_PROP_REF_ID, WS_SIGNATURE_PROPERTIES_KEY);
        inConfigProperties.put(WS_SIGNATURE_PROPERTIES_KEY, signatureProperties);
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

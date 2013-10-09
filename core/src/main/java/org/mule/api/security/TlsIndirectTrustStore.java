/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import java.io.IOException;

/**
 * Configure indirect trust stores.
 * TLS/SSL connections are made to trusted systems - the public certificates of trusted systems are store in 
 * a keystore (called a trust store) and used to verify that the connection made to a remote system "really
 * is" the expected identity.
 * 
 * <p>The information specified in this interface may be used to configure a trust store directly, as
 * part of {@link TlsDirectKeyStore}, or it may be stored as property values and used later, or both.  
 * It may therefore be specific to a single
 * connector, or global to all connectors made by that protocol, or even (in the case of the SSL transport)
 * become a global default value.  For more information see the documentation for the connector or protocol in
 * question.  The comments in {@link org.mule.api.security.tls.TlsConfiguration} may also be useful.</p>
 */
public interface TlsIndirectTrustStore
{

    /**
     * @return The location (resolved relative to the current classpath and file system, if possible)
     * of the keystore that contains public certificates of trusted servers.
     */
    String getTrustStore();

    /**
     * @param name The location of the keystore that contains public certificates of trusted servers.
     * @throws IOException If the location cannot be resolved via the file system or classpath
     */
    void setTrustStore(String name) throws IOException;

    /**
     * @return The password used to protected the trust store defined in {@link #getTrustStore()}
     */
    String getTrustStorePassword();

    /**
     * @param trustStorePassword The password used to protected the trust store defined in 
     * {@link #setTrustStore(String)}
     */
    void setTrustStorePassword(String trustStorePassword);

}



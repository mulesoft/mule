/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import java.io.IOException;

/**
 * Configure indirect key stores.
 * TLS/SSL connections are made on behalf of an entity, which can be anonymous or identified by a 
 * certificate - this interface specifies how a keystore can be used to provide the certificates
 * (and associated private keys) necessary for identification.
 * 
 * <p>The information specified in this interface is used to configure a key store indirectly.
 * For more information see the documentation for the connector or protocol in question.
 * The comments in {@link org.mule.api.security.tls.TlsConfiguration} may also be useful.</p>
 * 
 * <p><em>Programmers:</em> this information, once stored in and retrieved from properties via
 * {@link org.mule.api.security.tls.TlsPropertiesMapper}, will provide a key manager factory via the {@link TlsDirectKeyStore}
 * interface implemented by {@link org.mule.api.security.tls.TlsConfiguration}.  This can be associated with a socket
 * factory via {@link org.mule.api.security.tls.TlsPropertiesSocketFactory}.</p>
 */
public interface TlsIndirectKeyStore
{
    
    /**
     * @return The location (resolved relative to the current classpath and file system, if possible)
     * of the keystore that contains public certificates and private keys for identification.
     */
    String getClientKeyStore();

    /**
     * @param name The location of the keystore that contains public certificates  and private keys 
     * for identification.
     * @throws IOException If the location cannot be resolved via the file system or classpath
     */
    void setClientKeyStore(String name) throws IOException;

    /**
     * @return The password used to protect the keystore itself
     */
    String getClientKeyStorePassword();

    /**
     * @param clientKeyStorePassword The password used to protect the keystore itself
     */
    void setClientKeyStorePassword(String clientKeyStorePassword);

    void setClientKeyStoreType(String clientKeyStoreType);

    /**
     * @return The type of keystore used in {@link #setClientKeyStore(String)}
     */
    String getClientKeyStoreType();

}



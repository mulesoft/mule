/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.provider.AutoDiscoverySecurityProviderFactory;
import org.mule.umo.security.provider.SecurityProviderFactory;
import org.mule.umo.security.provider.SecurityProviderInfo;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for managing TLS connections.  Use as a delegate.  
 * See secure HTTP and email connectors for examples.
 * Pulled almost directly from {@link org.mule.providers.http.HttpsConnector}
 */
public final class TlsSupport
{

    public static final String DEFAULT_KEYSTORE = ".keystore";
    public static final String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();
    public static final String DEFAULT_SSL_TYPE = "SSLv3";

    private Log logger = LogFactory.getLog(getClass());
    private SecurityProviderFactory spFactory = new AutoDiscoverySecurityProviderFactory();
    private SecurityProviderInfo spInfo = spFactory.getSecurityProviderInfo();
    private String keyStore;
    private String keyPassword = null;
    private String storePassword = null;
    private String keystoreType = DEFAULT_KEYSTORE_TYPE;
    private String keyManagerAlgorithm = spInfo.getKeyManagerAlgorithm();
    private String sslType = DEFAULT_SSL_TYPE;
    private Provider provider = spFactory.getProvider();
    private String protocolHandler = spInfo.getProtocolHandler();
    private String clientKeyStore = null;
    private String clientKeyStorePassword = null;
    private String trustStore = null;
    private String trustStorePassword = null;
    private String trustStoreType = DEFAULT_KEYSTORE_TYPE;
    // default to key manager algorithm, overridable
    private String trustManagerAlgorithm = spInfo.getKeyManagerAlgorithm();
    private TrustManagerFactory trustManagerFactory;
    private boolean explicitTrustStoreOnly = false;
    private KeyManagerFactory keyManagerFactory = null;
    private boolean requireClientAuthentication = false;

    
    /**
     * Support for TLS connections with a default initial value for the key store
     * (normal use)
     */
    public TlsSupport()
    {
        this(DEFAULT_KEYSTORE);
    }
    
    /**
     * Support for TLS connections with a given initial value for the key store
     * @param keySTore initiall value for the key store
     */
    public TlsSupport(String keyStore)
    {
        this.keyStore = keyStore;
    }
    
    /**
     * This should be called before the connector's superclass is initialised
     * @param anon If the connection is anonymous then we don't care about client keys
     * @throws InitialisationException
     */
    public void initialiseFactories(boolean anon) throws InitialisationException
    {
        assertNotNull(getProvider(), "The security provider cannot be null");
        if (! anon)
        {
            assertNotNull(getKeyStore(), "The KeyStore location cannot be null");
            assertNotNull(getKeyPassword(), "The Key password cannot be null");
            assertNotNull(getStorePassword(), "The KeyStore password cannot be null");
            assertNotNull(getKeyManagerAlgorithm(), "The Key Manager Algorithm cannot be null");
            initKeyManagerFactory();
        }

        initTrustManagerFactory();
    }

    /**
     * This should be called after the connector's superclass is initialised
     * @throws InitialisationException
     */
    public void initialiseStores() throws InitialisationException 
    {
        if (protocolHandler != null)
        {
            System.setProperty("java.protocol.handler.pkgs", protocolHandler);
        }
        initClientKeyStore();
        initTrustStore();
    }

    public String getKeyStore()
    {
        return keyStore;
    }

    public void setKeyStore(String keyStore)
    {
        this.keyStore = keyStore;
    }

    public String getKeyPassword()
    {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword)
    {
        this.keyPassword = keyPassword;
    }

    public String getStorePassword()
    {
        return storePassword;
    }

    public void setStorePassword(String storePassword)
    {
        this.storePassword = storePassword;
    }

    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType)
    {
        this.trustStoreType = trustStoreType;
    }

    public TrustManagerFactory getTrustManagerFactory()
    {
        return trustManagerFactory;
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory)
    {
        this.trustManagerFactory = trustManagerFactory;
    }

    public String getTrustManagerAlgorithm()
    {
        return trustManagerAlgorithm;
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        this.trustManagerAlgorithm = trustManagerAlgorithm;
    }

    public String getKeystoreType()
    {
        return keystoreType;
    }

    public void setKeystoreType(String keystoreType)
    {
        this.keystoreType = keystoreType;
    }

    public String getKeyManagerAlgorithm()
    {
        return keyManagerAlgorithm;
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        this.keyManagerAlgorithm = keyManagerAlgorithm;
    }

    public String getSslType()
    {
        return sslType;
    }

    public void setSslType(String sslType)
    {
        this.sslType = sslType;
    }

    public boolean isRequireClientAuthentication()
    {
        return requireClientAuthentication;
    }

    public void setRequireClientAuthentication(boolean requireClientAuthentication)
    {
        this.requireClientAuthentication = requireClientAuthentication;
    }

    public KeyManagerFactory getKeyManagerFactory()
    {
        return keyManagerFactory;
    }

    public Provider getProvider()
    {
        return provider;
    }

    public void setProvider(Provider provider)
    {
        this.provider = provider;
    }

    public String getProtocolHandler()
    {
        return protocolHandler;
    }

    public void setProtocolHandler(String protocolHandler)
    {
        this.protocolHandler = protocolHandler;
    }

    public String getClientKeyStore()
    {
        return clientKeyStore;
    }

    public void setClientKeyStore(String clientKeyStore) throws IOException
    {
        this.clientKeyStore = clientKeyStore;
        if (this.clientKeyStore != null)
        {
            this.clientKeyStore = FileUtils.getResourcePath(clientKeyStore, getClass());
            logger.debug("Normalised clientKeyStore path to: " + getClientKeyStore());
        }
    }

    public String getClientKeyStorePassword()
    {
        return clientKeyStorePassword;
    }

    public void setClientKeyStorePassword(String clientKeyStorePassword)
    {
        this.clientKeyStorePassword = clientKeyStorePassword;
    }

    public String getTrustStore()
    {
        return trustStore;
    }

    public void setTrustStore(String trustStore) throws IOException
    {
        this.trustStore = trustStore;
        if (this.trustStore != null)
        {
            this.trustStore = FileUtils.getResourcePath(trustStore, getClass());
            logger.debug("Normalised trustStore path to: " + getTrustStore());
        }
    }

    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword)
    {
        this.trustStorePassword = trustStorePassword;
    }

    public boolean isExplicitTrustStoreOnly()
    {
        return explicitTrustStoreOnly;
    }

    public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly)
    {
        this.explicitTrustStoreOnly = explicitTrustStoreOnly;
    }

    public SecurityProviderFactory getSecurityProviderFactory()
    {
        return spFactory;
    }

    public void setSecurityProviderFactory(SecurityProviderFactory spFactory)
    {
        this.spFactory = spFactory;
    }

    private static void assertNotNull(Object value, String message)
    {
        if (null == value)
        {
            throw new NullPointerException(message);
        }
    }

    private void initKeyManagerFactory() throws InitialisationException
    {
        KeyStore store;
        try
        {
            Security.addProvider(getProvider());
            // Create keyStore
            store = KeyStore.getInstance(keystoreType);
            InputStream is = IOUtils.getResourceAsStream(getKeyStore(), getClass());
            if (is == null)
            {
                throw new FileNotFoundException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                    "Keystore: " + getKeyStore()).getMessage());
            }
            store.load(is, getStorePassword().toCharArray());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "KeyStore: "
                + getKeyStore()), e, this);
        }
        try
        {
            // Get key manager
            keyManagerFactory = KeyManagerFactory.getInstance(getKeyManagerAlgorithm());
            // Initialize the KeyManagerFactory to work with our keyStore
            keyManagerFactory.init(store, getKeyPassword().toCharArray());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Key Manager"), e, this);
        }
    }

    private void initTrustManagerFactory() throws InitialisationException 
    {
        if (getTrustStore() != null)
        {
            KeyStore truststore;
            try
            {
                truststore = KeyStore.getInstance(trustStoreType);
                InputStream is = IOUtils.getResourceAsStream(getTrustStore(), getClass());
                if (is == null)
                {
                    throw new FileNotFoundException(
                        "Failed to load truststore from classpath or local file: " + getTrustStore());
                }
                truststore.load(is, getTrustStorePassword().toCharArray());
            }
            catch (Exception e)
            {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "TrustStore: "
                    + getTrustStore()), e,
                    this);
            }

            try
            {
                trustManagerFactory = TrustManagerFactory.getInstance(getTrustManagerAlgorithm());
                trustManagerFactory.init(truststore);
            }
            catch (Exception e)
            {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X,
                    "Trust Manager (" + getTrustManagerAlgorithm() + ")"), e, this);
            }
        }
    }

    private void initClientKeyStore() throws InitialisationException
    {
        if (clientKeyStore != null)
        {
            try
            {
                String clientPath = FileUtils.getResourcePath(clientKeyStore, getClass());
                if (clientPath == null)
                {
                    throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "ClientKeyStore: "
                        + clientKeyStore),
                        this);
                }
                System.setProperty("javax.net.ssl.keyStore", clientPath);
                System.setProperty("javax.net.ssl.keyStorePassword", clientKeyStorePassword);

                logger.info("Set Client Key store: javax.net.ssl.keyStore=" + clientPath);
            }
            catch (IOException e)
            {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "ClientKeyStore: "
                    + clientKeyStore), this);
            }
        }
    }

    private void initTrustStore() 
    {
        if (trustStore != null)
        {
            System.setProperty("javax.net.ssl.trustStore", getTrustStore());
            if (getTrustStorePassword() != null)
            {
                System.setProperty("javax.net.ssl.trustStorePassword", getTrustStorePassword());
            }
            logger.debug("Set Trust store: javax.net.ssl.trustStore=" + getTrustStore());
        }
        else if (!isExplicitTrustStoreOnly())
        {
            logger.info("Defaulting trust store to client Key Store");
            trustStore = getClientKeyStore();
            trustStorePassword = getClientKeyStorePassword();
            if (trustStore != null)
            {
                System.setProperty("javax.net.ssl.trustStore", getTrustStore());
                System.setProperty("javax.net.ssl.trustStorePassword", getTrustStorePassword());
                logger.debug("Set Trust store: javax.net.ssl.trustStore=" + getTrustStore());
            }
        }
    }

}



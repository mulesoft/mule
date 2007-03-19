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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for managing TLS connections.  Use as a delegate.  
 * See secure HTTP and email connectors for examples.
 * Pulled almost directly from {@link org.mule.providers.http.HttpsConnector}
 */
public final class TlsConfiguration
{

    public static final String DEFAULT_KEYSTORE = ".keystore";
    public static final String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();
    public static final String DEFAULT_SSL_TYPE = "SSLv3";
    public static final String JSSE_NAMESPACE = "javax.net";

    private Log logger = LogFactory.getLog(getClass());

    private SecurityProviderFactory spFactory = new AutoDiscoverySecurityProviderFactory();
    private SecurityProviderInfo spInfo = spFactory.getSecurityProviderInfo();
    private Provider provider = spFactory.getProvider();
    private String sslType = DEFAULT_SSL_TYPE;

    // global
    private String protocolHandler = spInfo.getProtocolHandler();

    // this is the key store that is generated in-memory and available to connectors explicitly.
    // it is local to the socket.
    private String keyStoreName = null;
    private String keyPassword = null;
    private String keyStorePassword = null;
    private String keystoreType = DEFAULT_KEYSTORE_TYPE;
    private String keyManagerAlgorithm = spInfo.getKeyManagerAlgorithm();
    private KeyManagerFactory keyManagerFactory = null;

    // this is the key store defined in system properties that is used implicitly.
    // note that some transports use different namespaces within system properties,
    // so this is typically global across a particular transport.
    // it is also used as the trust store defined in system properties if no other trust
    // store is given and explicitTrustStoreOnly is false
    private String clientKeyStoreName = null;
    private String clientKeyStorePassword = null;
    private String clientKeyStoreType = DEFAULT_KEYSTORE_TYPE;

    // this is the trust store used to construct sockets both explicitly
    // and globally (if not set, see client key above) via the jvm defaults.
    private String trustStoreName = null;
    private String trustStorePassword = null;
    private String trustStoreType = DEFAULT_KEYSTORE_TYPE;
    private String trustManagerAlgorithm = spInfo.getKeyManagerAlgorithm();
    private TrustManagerFactory trustManagerFactory = null;
    private boolean explicitTrustStoreOnly = false;
    private boolean requireClientAuthentication = false;


    /**
     * Support for TLS connections with a given initial value for the key store
     * @param keyStore initial value for the key store
     */
    public TlsConfiguration(String keyStore)
    {
        this.keyStoreName = keyStore;
    }

    // note - in what follows i'm using "raw" variables rather than accessors because
    // i think the names are clearer.  the API names for the accessors are historical
    // and not a close fit to actual use (imho).

    /**
     * @param anon If the connection is anonymous then we don't care about client keys
     * @param namespace Namespace to use for global properties (for JSSE use JSSE_NAMESPACE)
     * @throws InitialisationException
     */
    public void initialise(boolean anon, String namespace) throws InitialisationException
    {
        logger.debug("initialising: anon " + anon);
        validate(anon);

        Security.addProvider(provider);
        System.setProperty("java.protocol.handler.pkgs", protocolHandler);

        if (! anon) 
        {
            initKeyManagerFactory();
        }
        initTrustManagerFactory();

        new TlsPropertiesMapper(namespace).writeToProperties(System.getProperties(), this);
    }

    private void validate(boolean anon) throws InitialisationException
    {
        assertNotNull(getProvider(), "The security provider cannot be null");
        if (! anon)
        {
            assertNotNull(getKeyStore(), "The KeyStore location cannot be null");
            assertNotNull(getKeyPassword(), "The Key password cannot be null");
            assertNotNull(getStorePassword(), "The KeyStore password cannot be null");
            assertNotNull(getKeyManagerAlgorithm(), "The Key Manager Algorithm cannot be null");
        }
    }

    private void initKeyManagerFactory() throws InitialisationException
    {
        logger.debug("initialising key manager factory from keystore data");
        KeyStore tempKeyStore;
        try
        {
            tempKeyStore = KeyStore.getInstance(keystoreType);
            InputStream is = IOUtils.getResourceAsStream(keyStoreName, getClass());
            if (null == is)
            {
                throw new FileNotFoundException(new Message(Messages.CANT_LOAD_X_FROM_CLASSPATH_FILE,
                    "Keystore: " + keyStoreName).getMessage());
            }
            tempKeyStore.load(is, keyStorePassword.toCharArray());
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                new Message(Messages.FAILED_LOAD_X, "KeyStore: " + keyStoreName), 
                e, this);
        }
        try
        {
            keyManagerFactory = KeyManagerFactory.getInstance(getKeyManagerAlgorithm());
            keyManagerFactory.init(tempKeyStore, keyPassword.toCharArray());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Key Manager"), e, this);
        }
    }


    // trust manager logic
    // use name, type, password and algorithm either directly or via properties

    private void initTrustManagerFactory() throws InitialisationException 
    {
        if (null != trustStoreName)
        {
            trustManagerFactory = buildTrustManagerFactory(
                trustStoreName, trustStoreType, trustStorePassword, trustManagerAlgorithm, this);
        }
    }

    private static TrustManagerFactory 
    buildTrustManagerFactory(String name, String type, String password, String algorithm, Object component) 
    throws InitialisationException
    {
        password = null == password ? "" : password;

        KeyStore trustStore;
        try
        {
            trustStore = KeyStore.getInstance(type);
            InputStream is = IOUtils.getResourceAsStream(name, component.getClass());
            if (null == is)
            {
                throw new FileNotFoundException(
                    "Failed to load truststore from classpath or local file: " + name);
            }
            trustStore.load(is, password.toCharArray());
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                new Message(Messages.FAILED_LOAD_X, "TrustStore: " + name), 
                e, component);
        }

        TrustManagerFactory trustFactory;
        try
        {
            trustFactory = TrustManagerFactory.getInstance(algorithm);
            trustFactory.init(trustStore);
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                new Message(Messages.FAILED_LOAD_X, "Trust Manager (" + algorithm + ")"), 
                e, component);
        }

        return trustFactory;
    }


    private static void assertNotNull(Object value, String message)
    {
        if (null == value)
        {
            throw new NullPointerException(message);
        }
    }

    private static String defaultForNull(String value, String deflt)
    {
        if (null == value)
        {
            return deflt;
        }
        else
        {
            return value;
        }
    }


    public SSLSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException
    {
        KeyManager[] keyManagers = 
            null == getKeyManagerFactory() ? null : getKeyManagerFactory().getKeyManagers();
        TrustManager[] trustManagers =
            null == getTrustManagerFactory() ? null :  getTrustManagerFactory().getTrustManagers();

        SSLContext context = SSLContext.getInstance(getSslType());
        context.init(keyManagers, trustManagers, null);
        return context.getSocketFactory();
    }


    public String getSslType()
    {
        return sslType;
    }

    public void setSslType(String sslType)
    {
        this.sslType = sslType;
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

    public SecurityProviderFactory getSecurityProviderFactory()
    {
        return spFactory;
    }

    public void setSecurityProviderFactory(SecurityProviderFactory spFactory)
    {
        this.spFactory = spFactory;
    }


    // access to the explicit key store variables

    public String getKeyStore()
    {
        return keyStoreName;
    }

    public void setKeyStore(String name) throws IOException
    {
        keyStoreName = name;
        if (null != keyStoreName)
        {
            keyStoreName = FileUtils.getResourcePath(keyStoreName, getClass());
            logger.debug("Normalised keyStore path to: " + keyStoreName);
        }
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
        return keyStorePassword;
    }

    public void setStorePassword(String storePassword)
    {
        this.keyStorePassword = storePassword;
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

    public KeyManagerFactory getKeyManagerFactory()
    {
        return keyManagerFactory;
    }


    // access to the implicit key store variables

    public String getClientKeyStore()
    {
        return clientKeyStoreName;
    }

    public void setClientKeyStore(String name) throws IOException
    {
        clientKeyStoreName = name;
        if (null != clientKeyStoreName)
        {
            clientKeyStoreName = FileUtils.getResourcePath(clientKeyStoreName, getClass());
            logger.debug("Normalised clientKeyStore path to: " + clientKeyStoreName);
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

    public void setClientKeyStoreType(String clientKeyStoreType)
    {
        this.clientKeyStoreType = clientKeyStoreType;
    }

    public String getClientKeyStoreType()
    {
        return clientKeyStoreType;
    }


    // access to trust store variables

    public String getTrustStore()
    {
        return trustStoreName;
    }

    public void setTrustStore(String name) throws IOException
    {
        trustStoreName = name;
        if (null != trustStoreName)
        {
            trustStoreName = FileUtils.getResourcePath(trustStoreName, getClass());
            logger.debug("Normalised trustStore path to: " + trustStoreName);
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

    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    public void setTrustStoreType(String trustStoreType)
    {
        this.trustStoreType = trustStoreType;
    }

    public String getTrustManagerAlgorithm()
    {
        return trustManagerAlgorithm;
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        this.trustManagerAlgorithm = defaultForNull(trustManagerAlgorithm, spInfo.getKeyManagerAlgorithm());
    }

    public TrustManagerFactory getTrustManagerFactory()
    {
        return trustManagerFactory;
    }

    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory)
    {
        this.trustManagerFactory = trustManagerFactory;
    }

    public boolean isExplicitTrustStoreOnly()
    {
        return explicitTrustStoreOnly;
    }

    public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly)
    {
        this.explicitTrustStoreOnly = explicitTrustStoreOnly;
    }

    public boolean isRequireClientAuthentication()
    {
        return requireClientAuthentication;
    }

    public void setRequireClientAuthentication(boolean requireClientAuthentication)
    {
        this.requireClientAuthentication = requireClientAuthentication;
    }

}



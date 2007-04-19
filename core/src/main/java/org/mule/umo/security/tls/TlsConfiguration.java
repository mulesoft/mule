/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security.tls;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.TlsDirectKeyStore;
import org.mule.umo.security.TlsDirectTrustStore;
import org.mule.umo.security.TlsIndirectKeyStore;
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
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for configuring TLS/SSL connections.
 * 
 * <h2>Introduction</h2>
 * 
 * This class was introduced to centralise the work of TLS/SSL configuration.  It is intended
 * to be backwards compatible with earlier code (as much as possible) and so is perhaps more 
 * complex than would be necessary if starting from zero - the main source of confusion is the
 * distinction between direct and indirect creation of sockets and stores.
 * 
 * <h2>Configuration</h2>
 * 
 * The documentation in this class is intended more for programmers than end uses.  If you are
 * configuring a connector the interfaces {@link org.mule.umo.security.TlsIndirectTrustStore},
 * {@link TlsDirectTrustStore},
 * {@link TlsDirectKeyStore} and {@link TlsIndirectKeyStore} should provide guidance to individual 
 * properties.  In addition you should check the documentation for the specific protocol / connector 
 * used and may also need to read the discussion on direct and indirect socket and store creation
 * below (or, more simply, just use whichever key store interface your connector implements!).
 * 
 * <h2>Programming</h2>
 * 
 * This class is intended to be used as a delegate as we typically want to add security to an
 * already existing connector (so we inherit from that connector, implement the appropriate
 * interfaces from {@link org.mule.umo.security.TlsIndirectTrustStore}, {@link TlsDirectTrustStore},
 * {@link TlsDirectKeyStore} and {@link TlsIndirectKeyStore}, and then forward calls to the 
 * interfaces to an instance of this class).
 * 
 * <p>For setting System properties (and reading them) use {@link TlsPropertiesMapper}.  This
 * can take a "namespace" which can then be used by {@link TlsPropertiesSocketFactory} to
 * construct an appropriate socket factory.  This approach (storing to proeprties and then
 * retrieving that information later in a socket factory) lets us pass TLS/SSL configuration
 * into libraries that are configured by specifying on the socket factory class.</p>
 * 
 * <h2>Direct and Indirect Socket and Store Creation</h2>
 * 
 * For the SSL transport, which historically defined parameters for many different secure
 * transports, the configuration interfaces worked as follows:
 * 
 * <dl>
 * <dt>{@link TlsDirectTrustStore}</dt><dd>Used to generate trust store directly and indirectly 
 * for all TLS/SSL conections via System properties</dd>
 * <dt>{@link TlsDirectKeyStore}</dt><dd>Used to generate key store directly</dd>
 * <dt>{@link TlsIndirectKeyStore}</dt><dd>Used to generate key store indirectly for all
 * TLS/SSL conections via System properties</dd>
 * </dl>
 * 
 * Historically, many other transports relied on the indirect configurations defined above.
 * So they implemented {@link org.mule.umo.security.TlsIndirectTrustStore}
 * (a superclass of {@link TlsDirectTrustStore})
 * and relied on {@link TlsIndirectKeyStore} from the SSL configuration.  For continuity these
 * interfaces continue to be used, even though 
 * the configurations are now typically (see individual connector/protocol documentation) specific 
 * to a protocol or connector.  <em>Note - these interfaces are new, but the original code had
 * those methods, used as described.  The new interfaces only make things explicit.</em>
 * 
 * <p><em>Note for programmers</em> One way to understand the above is to see that many
 * protocols are handled by libraries that are configured by providing either properties or
 * a socket factory.  In both cases (the latter via {@link TlsPropertiesSocketFactory}) we
 * continue to use properties and the "indirect" interface.  Note also that the mapping
 * in {@link TlsPropertiesMapper} correctly handles the asymmetry, so an initial call to
 * {@link TlsConfiguration} uses the keystore defined via {@link TlsDirectKeyStore}, but
 * when a {@link TlsConfiguration} is retrieved from System proerties using 
 * {@link TlsPropertiesMapper#readFromProperties(TlsConfiguration, java.util.Properties)}
 * the "indirect" properties are supplied as "direct" values, meaning that the "indirect"
 * socket factory can be retrieved from {@link #getKeyManagerFactory()}.  It just works.</p>
 */
public final class TlsConfiguration implements TlsDirectTrustStore, TlsDirectKeyStore, TlsIndirectKeyStore
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
    private String keyStoreName = DEFAULT_KEYSTORE; // was default in https but not ssl
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
     * @throws InitialisationException ON initialisation problems
     */
    public void initialise(boolean anon, String namespace) throws InitialisationException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("initialising: anon " + anon);
        }
        validate(anon);

        Security.addProvider(provider);
        System.setProperty("java.protocol.handler.pkgs", protocolHandler);

        if (!anon) 
        {
            initKeyManagerFactory();
        }
        initTrustManagerFactory();

        if (null != namespace)
        {
            new TlsPropertiesMapper(namespace).writeToProperties(System.getProperties(), this);
        }
    }

    private void validate(boolean anon) throws InitialisationException
    {
        assertNotNull(getProvider(), "The security provider cannot be null");
        if (!anon)
        {
            assertNotNull(getKeyStore(), "The KeyStore location cannot be null");
            assertNotNull(getKeyPassword(), "The Key password cannot be null");
            assertNotNull(getStorePassword(), "The KeyStore password cannot be null");
            assertNotNull(getKeyManagerAlgorithm(), "The Key Manager Algorithm cannot be null");
        }
    }

    private void initKeyManagerFactory() throws InitialisationException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("initialising key manager factory from keystore data");
        }
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

    private void initTrustManagerFactory() throws InitialisationException 
    {
        if (null != trustStoreName)
        {
            trustStorePassword = null == trustStorePassword ? "" : trustStorePassword;

            KeyStore trustStore;
            try
            {
                trustStore = KeyStore.getInstance(trustStoreType);
                InputStream is = IOUtils.getResourceAsStream(trustStoreName, getClass());
                if (null == is)
                {
                    throw new FileNotFoundException(
                        "Failed to load truststore from classpath or local file: " + trustStoreName);
                }
                trustStore.load(is, trustStorePassword.toCharArray());
            }
            catch (Exception e)
            {
                throw new InitialisationException(
                    new Message(Messages.FAILED_LOAD_X, "TrustStore: " + trustStoreName), 
                    e, this);
            }

            try
            {
                trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
                trustManagerFactory.init(trustStore);
            }
            catch (Exception e)
            {
                throw new InitialisationException(
                    new Message(Messages.FAILED_LOAD_X, "Trust Manager (" + trustManagerAlgorithm + ")"), 
                    e, this);
            }
        }
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
        return getSslContext().getSocketFactory();
    }

    public SSLServerSocketFactory getServerSocketFactory()
            throws NoSuchAlgorithmException, KeyManagementException
    {
        return getSslContext().getServerSocketFactory();
    }

    public SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException
    {
        KeyManager[] keyManagers =
            null == getKeyManagerFactory() ? null : getKeyManagerFactory().getKeyManagers();
        TrustManager[] trustManagers =
            null == getTrustManagerFactory() ? null :  getTrustManagerFactory().getTrustManagers();

        SSLContext context = SSLContext.getInstance(getSslType());
        // TODO - nice to have a configurable random number source set here
        context.init(keyManagers, trustManagers, null);
        return context;
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Normalised keyStore path to: " + keyStoreName);
            }
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Normalised clientKeyStore path to: " + clientKeyStoreName);
            }
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
            if (logger.isDebugEnabled())
            {
                logger.debug("Normalised trustStore path to: " + trustStoreName);
            }
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



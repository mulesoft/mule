/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security.tls;

import static java.lang.String.format;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.mule.api.lifecycle.CreateException;
import org.mule.api.security.TlsDirectKeyStore;
import org.mule.api.security.TlsDirectTrustStore;
import org.mule.api.security.TlsIndirectKeyStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ArrayUtils;
import org.mule.util.FileUtils;
import org.mule.util.IOUtils;
import org.mule.util.SecurityUtils;
import org.mule.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRL;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXRevocationChecker;
import java.security.cert.PKIXRevocationChecker.Option;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Support for configuring TLS/SSL connections.
 * <p/>
 * <h2>Introduction</h2>
 * <p/>
 * This class was introduced to centralise the work of TLS/SSL configuration.  It is intended
 * to be backwards compatible with earlier code (as much as possible) and so is perhaps more
 * complex than would be necessary if starting from zero - the main source of confusion is the
 * distinction between direct and indirect creation of sockets and stores.
 * <p/>
 * <h2>Configuration</h2>
 * <p/>
 * The documentation in this class is intended more for programmers than end uses.  If you are
 * configuring a connector the interfaces {@link org.mule.api.security.TlsIndirectTrustStore},
 * {@link TlsDirectTrustStore},
 * {@link TlsDirectKeyStore} and {@link TlsIndirectKeyStore} should provide guidance to individual
 * properties.  In addition you should check the documentation for the specific protocol / connector
 * used and may also need to read the discussion on direct and indirect socket and store creation
 * below (or, more simply, just use whichever key store interface your connector implements!).
 * <p/>
 * <h2>Programming</h2>
 * <p/>
 * This class is intended to be used as a delegate as we typically want to add security to an
 * already existing connector (so we inherit from that connector, implement the appropriate
 * interfaces from {@link org.mule.api.security.TlsIndirectTrustStore}, {@link TlsDirectTrustStore},
 * {@link TlsDirectKeyStore} and {@link TlsIndirectKeyStore}, and then forward calls to the
 * interfaces to an instance of this class).
 * <p/>
 * <p>For setting System properties (and reading them) use {@link TlsPropertiesMapper}.  This
 * can take a "namespace" which can then be used by {@link TlsPropertiesSocketFactory} to
 * construct an appropriate socket factory.  This approach (storing to properties and then
 * retrieving that information later in a socket factory) lets us pass TLS/SSL configuration
 * into libraries that are configured by specifying on the socket factory class.</p>
 * <p/>
 * <h2>Direct and Indirect Socket and Store Creation</h2>
 * <p/>
 * For the SSL transport, which historically defined parameters for many different secure
 * transports, the configuration interfaces worked as follows:
 * <p/>
 * <dl>
 * <dt>{@link TlsDirectTrustStore}</dt><dd>Used to generate trust store directly and indirectly
 * for all TLS/SSL conections via System properties</dd>
 * <dt>{@link TlsDirectKeyStore}</dt><dd>Used to generate key store directly</dd>
 * <dt>{@link TlsIndirectKeyStore}</dt><dd>Used to generate key store indirectly for all
 * TLS/SSL conections via System properties</dd>
 * </dl>
 * <p/>
 * Historically, many other transports relied on the indirect configurations defined above.
 * So they implemented {@link org.mule.api.security.TlsIndirectTrustStore}
 * (a superclass of {@link TlsDirectTrustStore})
 * and relied on {@link TlsIndirectKeyStore} from the SSL configuration.  For continuity these
 * interfaces continue to be used, even though
 * the configurations are now typically (see individual connector/protocol documentation) specific
 * to a protocol or connector.  <em>Note - these interfaces are new, but the original code had
 * those methods, used as described.  The new interfaces only make things explicit.</em>
 * <p/>
 * <p><em>Note for programmers</em> One way to understand the above is to see that many
 * protocols are handled by libraries that are configured by providing either properties or
 * a socket factory.  In both cases (the latter via {@link TlsPropertiesSocketFactory}) we
 * continue to use properties and the "indirect" interface.  Note also that the mapping
 * in {@link TlsPropertiesMapper} correctly handles the asymmetry, so an initial call to
 * {@link TlsConfiguration} uses the keystore defined via {@link TlsDirectKeyStore}, but
 * when a {@link TlsConfiguration} is retrieved from System proerties using
 * {@link TlsPropertiesMapper#readFromProperties(TlsConfiguration,java.util.Properties)}
 * the "indirect" properties are supplied as "direct" values, meaning that the "indirect"
 * socket factory can be retrieved from {@link #getKeyManagerFactory()}.  It just works.</p>
 */
public final class TlsConfiguration
        implements TlsDirectTrustStore, TlsDirectKeyStore, TlsIndirectKeyStore
{
    public static final String DEFAULT_KEYSTORE = ".keystore";
    public static final String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();
    public static final String DEFAULT_KEYMANAGER_ALGORITHM = KeyManagerFactory.getDefaultAlgorithm();
    public static final String DEFAULT_SSL_TYPE = "TLSv1";
    public static final String JSSE_NAMESPACE = "javax.net";

    public static final String PROPERTIES_FILE_PATTERN = "tls-%s.conf";
    public static final String DEFAULT_SECURITY_MODEL = "default";
    public static final String FIPS_SECURITY_MODEL = "fips140-2";

    public static final String DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY = SYSTEM_PROPERTY_PREFIX + "tls.disableSystemPropertiesMapping";

    private Log logger = LogFactory.getLog(getClass());

    private String sslType = DEFAULT_SSL_TYPE;

    // this is the key store that is generated in-memory and available to connectors explicitly.
    // it is local to the socket.
    private String keyStoreName = DEFAULT_KEYSTORE; // was default in https but not ssl
    private String keyAlias = null;
    private String keyPassword = null;
    private String keyStorePassword = null;
    private String keystoreType = DEFAULT_KEYSTORE_TYPE;
    private String keyManagerAlgorithm = DEFAULT_KEYMANAGER_ALGORITHM;
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
    private String trustManagerAlgorithm = DEFAULT_KEYMANAGER_ALGORITHM;
    private TrustManagerFactory trustManagerFactory = null;
    private boolean explicitTrustStoreOnly = false;
    private boolean requireClientAuthentication = false;

    private TlsProperties tlsProperties = new TlsProperties();
    private boolean disableSystemPropertiesMapping = true;

    // standard certificate revocation checking
    private Boolean rcStandardEnable = false;
    private Boolean rcStandardOnlyEndEntities = false;
    private Boolean rcStandardPreferCrls = false;
    private Boolean rcStandardNoFallback = false;
    private Boolean rcStandardSoftFail = false;

    // CRL file revocation checking
    private String rcCrlFilePath;

    // custom OCSP revocation checking
    private String rcCustomOcspUrl;
    private String rcCustomOcspCertAlias;

    /**
     * Support for TLS connections with a given initial value for the key store
     *
     * @param keyStore initial value for the key store
     */
    public TlsConfiguration(String keyStore)
    {
        this.keyStoreName = keyStore;

        String disableSystemPropertiesMappingValue = System.getProperty(DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY);

        if (disableSystemPropertiesMappingValue != null)
        {
            disableSystemPropertiesMapping = BooleanUtils.toBoolean(disableSystemPropertiesMappingValue);
        }
    }

    // note - in what follows i'm using "raw" variables rather than accessors because
    // i think the names are clearer.  the API names for the accessors are historical
    // and not a close fit to actual use (imho).

    /**
     * @param anon      If the connection is anonymous then we don't care about client keys
     * @param namespace Namespace to use for global properties (for JSSE use JSSE_NAMESPACE)
     * @throws CreateException ON initialisation problems
     */
    public void initialise(boolean anon, String namespace) throws CreateException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("initialising: anon " + anon);
        }
        validate(anon);


        if (!anon)
        {
            initKeyManagerFactory();
        }
        initTrustManagerFactory();

        if (logger.isDebugEnabled())
        {
            logger.debug("TLS system properties mapping is " + (disableSystemPropertiesMapping ? "disabled" : "enabled"));
        }

        if (null != namespace && !disableSystemPropertiesMapping)
        {
            new TlsPropertiesMapper(namespace).writeToProperties(System.getProperties(), this);
        }

        tlsProperties.load(format(PROPERTIES_FILE_PATTERN, SecurityUtils.getSecurityModel()));
    }

    private void validate(boolean anon) throws CreateException
    {
        if (!anon)
        {
            assertNotNull(getKeyStore(), "The KeyStore location cannot be null");
            assertNotNull(getKeyPassword(), "The Key password cannot be null");
            assertNotNull(getKeyStorePassword(), "The KeyStore password cannot be null");
            assertNotNull(getKeyManagerAlgorithm(), "The Key Manager Algorithm cannot be null");
        }
    }

    private void initKeyManagerFactory() throws CreateException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("initialising key manager factory from keystore data");
        }

        KeyStore tempKeyStore;
        try
        {
            tempKeyStore = loadKeyStore();
            checkKeyStoreContainsAlias(tempKeyStore);
        }
        catch (Exception e)
        {
            throw new CreateException(
                    CoreMessages.failedToLoad("KeyStore: " + keyStoreName), e, this);
        }

        try
        {
            keyManagerFactory = KeyManagerFactory.getInstance(getKeyManagerAlgorithm());
            keyManagerFactory.init(tempKeyStore, keyPassword.toCharArray());
        }
        catch (Exception e)
        {
            throw new CreateException(CoreMessages.failedToLoad("Key Manager"), e, this);
        }
    }

    protected  KeyStore loadKeyStore() throws GeneralSecurityException, IOException
    {
        KeyStore tempKeyStore = KeyStore.getInstance(keystoreType);

        InputStream is = IOUtils.getResourceAsStream(keyStoreName, getClass());
        if (null == is)
        {
            throw new FileNotFoundException(
                CoreMessages.cannotLoadFromClasspath("Keystore: " + keyStoreName).getMessage());
        }

        tempKeyStore.load(is, keyStorePassword.toCharArray());
        return tempKeyStore;
    }

    protected void checkKeyStoreContainsAlias(KeyStore keyStore) throws KeyStoreException
    {
        if (StringUtils.isNotBlank(keyAlias))
        {
            boolean keyAliasFound = false;

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements())
            {
                String alias = aliases.nextElement();

                if (alias.equals(keyAlias))
                {
                    // if alias is found all is valid but continue processing to strip out all
                    // other (unwanted) keys
                    keyAliasFound = true;
                }
                else
                {
                    // if the current alias is not the one we are looking for, remove
                    // it from the keystore
                    keyStore.deleteEntry(alias);
                }
            }

            // if the alias was not found, throw an exception
            if (!keyAliasFound)
            {
                throw new IllegalStateException("Key with alias \"" + keyAlias + "\" was not found");
            }
        }
    }

    private void initTrustManagerFactory() throws CreateException
    {
        if (null != trustStoreName)
        {
            KeyStore trustStore = createTrustStore();

            try
            {
                trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);

                Boolean revocationEnabled = getRcStandardEnable() || getRcCrlFilePath() != null || getRcCustomOcspUrl() != null;
                ManagerFactoryParameters tmfParams = null;

                // Revocation checking is only supported for PKIX algorithm
                if (revocationEnabled && "PKIX".equalsIgnoreCase(getTrustManagerAlgorithm()))
                {
                    if (getRcStandardEnable()) {
                        tmfParams = configForStandardRevocation(trustStore);

                    }
                    else if (getRcCrlFilePath() != null)
                    {
                        tmfParams = configForCrlFileRevocation(trustStore);

                    }
                    else if (getRcCustomOcspUrl() != null)
                    {
                        tmfParams = configForCustomOcspRevocation(trustStore);
                    }

                    trustManagerFactory.init(tmfParams);
                }
                else
                {
                    if (revocationEnabled && !"PKIX".equalsIgnoreCase(getTrustManagerAlgorithm()))
                    {
                        logger.warn(format("TLS Context: certificate revocation checking is not available when algorithm is different from PKIX (currently %s)", getTrustManagerAlgorithm()));
                    }

                    trustManagerFactory.init(trustStore);
                }
            }
            catch (Exception e)
            {
                throw new CreateException(
                        CoreMessages.failedToLoad("Trust Manager (" + trustManagerAlgorithm + ")"), e, this);
            }
        }
    }

    private KeyStore createTrustStore() throws CreateException
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
            throw new CreateException(
                    CoreMessages.failedToLoad("TrustStore: " + trustStoreName), e, this);
        }

        return trustStore;
    }

    private ManagerFactoryParameters configForStandardRevocation(KeyStore trustStore)
            throws NoSuchAlgorithmException, KeyStoreException, InvalidAlgorithmParameterException
    {
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker rc = (PKIXRevocationChecker) cpb.getRevocationChecker();

        Set<Option> options = new HashSet<>();
        if (getRcStandardOnlyEndEntities())
        {
            options.add(Option.ONLY_END_ENTITY);
        }
        if (getRcStandardPreferCrls())
        {
            options.add(Option.PREFER_CRLS);
        }
        if (getRcStandardNoFallback())
        {
            options.add(Option.NO_FALLBACK);
        }
        if (getRcStandardSoftFail())
        {
            options.add(Option.SOFT_FAIL);
        }
        rc.setOptions(options);

        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
        pkixParams.addCertPathChecker(rc);

        return new CertPathTrustManagerParameters(pkixParams);
    }

    private ManagerFactoryParameters configForCrlFileRevocation(KeyStore trustStore) throws Exception
    {
        // When creating build parameters we must manually trust each certificate (which is automatic otherwise)
        Enumeration<String> aliases = trustStore.aliases();
        HashSet<TrustAnchor> trustAnchors = new HashSet<>();
        while (aliases.hasMoreElements())
        {
            String alias = aliases.nextElement();
            if (trustStore.isCertificateEntry(alias))
            {
                trustAnchors.add(new TrustAnchor((X509Certificate) trustStore.getCertificate(alias), null));
            }
        }

        PKIXBuilderParameters pbParams = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());

        // Make sure revocation checking is enabled (com.sun.net.ssl.checkRevocation)
        pbParams.setRevocationEnabled(true);

        Collection<? extends CRL> crls = loadCRL(getRcCrlFilePath());
        if (crls != null && !crls.isEmpty())
        {
            pbParams.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(crls)));
        }

        return new CertPathTrustManagerParameters(pbParams);
    }

    private ManagerFactoryParameters configForCustomOcspRevocation(KeyStore trustStore)
            throws NoSuchAlgorithmException, KeyStoreException, InvalidAlgorithmParameterException, URISyntaxException
    {
        CertPathBuilder cpb = CertPathBuilder.getInstance("PKIX");
        PKIXRevocationChecker rc = (PKIXRevocationChecker) cpb.getRevocationChecker();
        rc.setOptions(EnumSet.of(Option.NO_FALLBACK));

        if (getRcCustomOcspUrl() != null)
        {
            rc.setOcspResponder(new URI(getRcCustomOcspUrl()));
        }
        if (getRcCustomOcspCertAlias() != null)
        {
            if (trustStore.isCertificateEntry(getRcCustomOcspCertAlias()))
            {
                rc.setOcspResponderCert((X509Certificate) trustStore.getCertificate(getRcCustomOcspCertAlias()));
            }
            else
            {
                throw new IllegalStateException("Key with alias \"" + getRcCustomOcspCertAlias() + "\" was not found");
            }
        }

        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustStore, new X509CertSelector());
        pkixParams.addCertPathChecker(rc);

        return new CertPathTrustManagerParameters(pkixParams);
    }

    public Collection<? extends CRL> loadCRL(String crlPath) throws Exception
    {
        Collection<? extends CRL> crlList = null;

        if (crlPath != null)
        {
            InputStream in = null;
            try
            {
                in = IOUtils.getResourceAsStream(crlPath, getClass());
                crlList = CertificateFactory.getInstance("X.509").generateCRLs(in);
            }
            finally
            {
                if (in != null)
                {
                    in.close();
                }
            }
        }

        return crlList;
    }


    private static void assertNotNull(Object value, String message)
    {
        if (null == value)
        {
            throw new IllegalArgumentException(message);
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
        return new RestrictedSSLSocketFactory(getSslContext(), getEnabledCipherSuites(), getEnabledProtocols());
    }

    public SSLServerSocketFactory getServerSocketFactory()
            throws NoSuchAlgorithmException, KeyManagementException
    {
        return new RestrictedSSLServerSocketFactory(getSslContext(), getEnabledCipherSuites(), getEnabledProtocols());
    }


    public String[] getEnabledCipherSuites()
    {
        return tlsProperties.getEnabledCipherSuites();
    }

    public String[] getEnabledProtocols()
    {
        return tlsProperties.getEnabledProtocols();
    }

    public SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException
    {
        TrustManager[] trustManagers =
                null == getTrustManagerFactory() ? null : getTrustManagerFactory().getTrustManagers();

        return getSslContext(trustManagers);
    }

    public SSLContext getSslContext(TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException
    {
        KeyManager[] keyManagers =
                null == getKeyManagerFactory() ? null : getKeyManagerFactory().getKeyManagers();

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
        String[] enabledProtocols = tlsProperties.getEnabledProtocols();

        if (enabledProtocols != null && !ArrayUtils.contains(enabledProtocols, sslType))
        {
            throw new IllegalArgumentException(format("Protocol %s is not allowed in current configuration", sslType));
        }

        this.sslType = sslType;
    }

    // access to the explicit key store variables

    @Override
    public String getKeyStore()
    {
        return keyStoreName;
    }

    @Override
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

    @Override
    public String getKeyPassword()
    {
        return keyPassword;
    }

    @Override
    public void setKeyPassword(String keyPassword)
    {
        this.keyPassword = keyPassword;
    }

    @Override
    public String getKeyStorePassword()
    {
        return keyStorePassword;
    }

    @Override
    public void setKeyStorePassword(String storePassword)
    {
        this.keyStorePassword = storePassword;
    }

    @Override
    public String getKeyStoreType()
    {
        return keystoreType;
    }

    @Override
    public void setKeyStoreType(String keystoreType)
    {
        this.keystoreType = keystoreType;
    }

    @Override
    public String getKeyManagerAlgorithm()
    {
        return keyManagerAlgorithm;
    }

    @Override
    public void setKeyManagerAlgorithm(String keyManagerAlgorithm)
    {
        this.keyManagerAlgorithm = keyManagerAlgorithm;
    }

    @Override
    public KeyManagerFactory getKeyManagerFactory()
    {
        return keyManagerFactory;
    }

    // access to the implicit key store variables

    @Override
    public String getClientKeyStore()
    {
        return clientKeyStoreName;
    }

    @Override
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

    @Override
    public String getClientKeyStorePassword()
    {
        return clientKeyStorePassword;
    }

    @Override
    public void setClientKeyStorePassword(String clientKeyStorePassword)
    {
        this.clientKeyStorePassword = clientKeyStorePassword;
    }

    @Override
    public void setClientKeyStoreType(String clientKeyStoreType)
    {
        this.clientKeyStoreType = clientKeyStoreType;
    }

    @Override
    public String getClientKeyStoreType()
    {
        return clientKeyStoreType;
    }

    // access to trust store variables

    @Override
    public String getTrustStore()
    {
        return trustStoreName;
    }

    @Override
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

    @Override
    public String getTrustStorePassword()
    {
        return trustStorePassword;
    }

    @Override
    public void setTrustStorePassword(String trustStorePassword)
    {
        this.trustStorePassword = trustStorePassword;
    }

    @Override
    public String getTrustStoreType()
    {
        return trustStoreType;
    }

    @Override
    public void setTrustStoreType(String trustStoreType)
    {
        this.trustStoreType = trustStoreType;
    }

    @Override
    public String getTrustManagerAlgorithm()
    {
        return trustManagerAlgorithm;
    }

    @Override
    public void setTrustManagerAlgorithm(String trustManagerAlgorithm)
    {
        this.trustManagerAlgorithm = defaultForNull(trustManagerAlgorithm, DEFAULT_KEYMANAGER_ALGORITHM);
    }

    @Override
    public TrustManagerFactory getTrustManagerFactory()
    {
        return trustManagerFactory;
    }

    @Override
    public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory)
    {
        this.trustManagerFactory = trustManagerFactory;
    }

    @Override
    public boolean isExplicitTrustStoreOnly()
    {
        return explicitTrustStoreOnly;
    }

    @Override
    public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly)
    {
        this.explicitTrustStoreOnly = explicitTrustStoreOnly;
    }

    @Override
    public boolean isRequireClientAuthentication()
    {
        return requireClientAuthentication;
    }

    @Override
    public void setRequireClientAuthentication(boolean requireClientAuthentication)
    {
        this.requireClientAuthentication = requireClientAuthentication;
    }

    @Override
    public String getKeyAlias()
    {
        return keyAlias;
    }

    @Override
    public void setKeyAlias(String keyAlias)
    {
        this.keyAlias = keyAlias;
    }

    public Boolean getRcStandardEnable()
    {
        return rcStandardEnable;
    }

    public void setRcStandardEnable(Boolean rcStandardEnable)
    {
        this.rcStandardEnable = rcStandardEnable;
    }

    public Boolean getRcStandardOnlyEndEntities()
    {
        return rcStandardOnlyEndEntities;
    }

    public void setRcStandardOnlyEndEntities(Boolean rcStandardOnlyEndEntities)
    {
        this.rcStandardOnlyEndEntities = rcStandardOnlyEndEntities;
    }

    public Boolean getRcStandardPreferCrls()
    {
        return rcStandardPreferCrls;
    }

    public void setRcStandardPreferCrls(Boolean rcStandardPreferCrls)
    {
        this.rcStandardPreferCrls = rcStandardPreferCrls;
    }

    public Boolean getRcStandardNoFallback()
    {
        return rcStandardNoFallback;
    }

    public void setRcStandardNoFallback(Boolean rcStandardNoFallback)
    {
        this.rcStandardNoFallback = rcStandardNoFallback;
    }

    public Boolean getRcStandardSoftFail()
    {
        return rcStandardSoftFail;
    }

    public void setRcStandardSoftFail(Boolean rcStandardSoftFail)
    {
        this.rcStandardSoftFail = rcStandardSoftFail;
    }

    public String getRcCrlFilePath()
    {
        return rcCrlFilePath;
    }

    public void setRcCrlFilePath(String rcCrlFilePath)
    {
        this.rcCrlFilePath = rcCrlFilePath;
    }

    public String getRcCustomOcspUrl()
    {
        return rcCustomOcspUrl;
    }

    public void setRcCustomOcspUrl(String rcCustomOcspUrl)
    {
        this.rcCustomOcspUrl = rcCustomOcspUrl;
    }

    public String getRcCustomOcspCertAlias()
    {
        return rcCustomOcspCertAlias;
    }

    public void setRcCustomOcspCertAlias(String rcCustomOcspCertAlias)
    {
        this.rcCustomOcspCertAlias = rcCustomOcspCertAlias;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof TlsConfiguration))
        {
            return false;
        }

        TlsConfiguration that = (TlsConfiguration) o;

        if (explicitTrustStoreOnly != that.explicitTrustStoreOnly)
        {
            return false;
        }
        if (requireClientAuthentication != that.requireClientAuthentication)
        {
            return false;
        }
        if (clientKeyStoreName != null ? !clientKeyStoreName.equals(that.clientKeyStoreName) : that.clientKeyStoreName != null)
        {
            return false;
        }
        if (clientKeyStorePassword != null ? !clientKeyStorePassword.equals(that.clientKeyStorePassword) : that.clientKeyStorePassword != null)
        {
            return false;
        }
        if (clientKeyStoreType != null ? !clientKeyStoreType.equals(that.clientKeyStoreType) : that.clientKeyStoreType != null)
        {
            return false;
        }
        if (keyAlias != null ? !keyAlias.equals(that.keyAlias) : that.keyAlias != null)
        {
            return false;
        }
        if (keyManagerAlgorithm != null ? !keyManagerAlgorithm.equals(that.keyManagerAlgorithm) : that.keyManagerAlgorithm != null)
        {
            return false;
        }
        if (keyManagerFactory != null ? !keyManagerFactory.equals(that.keyManagerFactory) : that.keyManagerFactory != null)
        {
            return false;
        }
        if (keyPassword != null ? !keyPassword.equals(that.keyPassword) : that.keyPassword != null)
        {
            return false;
        }
        if (keyStoreName != null ? !keyStoreName.equals(that.keyStoreName) : that.keyStoreName != null)
        {
            return false;
        }
        if (keyStorePassword != null ? !keyStorePassword.equals(that.keyStorePassword) : that.keyStorePassword != null)
        {
            return false;
        }
        if (keystoreType != null ? !keystoreType.equals(that.keystoreType) : that.keystoreType != null)
        {
            return false;
        }
        if (sslType != null ? !sslType.equals(that.sslType) : that.sslType != null)
        {
            return false;
        }
        if (tlsProperties != null ? !tlsProperties.equals(that.tlsProperties) : that.tlsProperties != null)
        {
            return false;
        }
        if (trustManagerAlgorithm != null ? !trustManagerAlgorithm.equals(that.trustManagerAlgorithm) : that.trustManagerAlgorithm != null)
        {
            return false;
        }
        if (trustManagerFactory != null ? !trustManagerFactory.equals(that.trustManagerFactory) : that.trustManagerFactory != null)
        {
            return false;
        }
        if (trustStoreName != null ? !trustStoreName.equals(that.trustStoreName) : that.trustStoreName != null)
        {
            return false;
        }
        if (trustStorePassword != null ? !trustStorePassword.equals(that.trustStorePassword) : that.trustStorePassword != null)
        {
            return false;
        }
        if (trustStoreType != null ? !trustStoreType.equals(that.trustStoreType) : that.trustStoreType != null)
        {
            return false;
        }
        if (rcStandardEnable != null ? !rcStandardEnable.equals(that.rcStandardEnable) : that.rcStandardEnable != null)
        {
            return false;
        }
        if (rcStandardOnlyEndEntities != null ? !rcStandardOnlyEndEntities.equals(that.rcStandardOnlyEndEntities) : that.rcStandardOnlyEndEntities != null)
        {
            return false;
        }
        if (rcStandardPreferCrls != null ? !rcStandardPreferCrls.equals(that.rcStandardPreferCrls) : that.rcStandardPreferCrls != null)
        {
            return false;
        }
        if (rcStandardNoFallback != null ? !rcStandardNoFallback.equals(that.rcStandardNoFallback) : that.rcStandardNoFallback != null)
        {
            return false;
        }
        if (rcStandardSoftFail != null ? !rcStandardSoftFail.equals(that.rcStandardSoftFail) : that.rcStandardSoftFail != null)
        {
            return false;
        }
        if (rcCrlFilePath != null ? !rcCrlFilePath.equals(that.rcCrlFilePath) : that.rcCrlFilePath != null)
        {
            return false;
        }
        if (rcCustomOcspUrl != null ? !rcCustomOcspUrl.equals(that.rcCustomOcspUrl) : that.rcCustomOcspUrl != null)
        {
            return false;
        }
        if (rcCustomOcspCertAlias != null ? !rcCustomOcspCertAlias.equals(that.rcCustomOcspCertAlias) : that.rcCustomOcspCertAlias != null)
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = sslType != null ? sslType.hashCode() : 0;
        int hashcodePrimeNumber = 31;
        result = hashcodePrimeNumber * result + (keyStoreName != null ? keyStoreName.hashCode() : 0);
        result = hashcodePrimeNumber * result + (keyAlias != null ? keyAlias.hashCode() : 0);
        result = hashcodePrimeNumber * result + (keyPassword != null ? keyPassword.hashCode() : 0);
        result = hashcodePrimeNumber * result + (keyManagerAlgorithm != null ? keyManagerAlgorithm.hashCode() : 0);
        result = hashcodePrimeNumber * result + (keyManagerFactory != null ? keyManagerFactory.hashCode() : 0);

        result = hashcodePrimeNumber * result + (keyStorePassword != null ? keyStorePassword.hashCode() : 0);
        result = hashcodePrimeNumber * result + (keystoreType != null ? keystoreType.hashCode() : 0);
        result = hashcodePrimeNumber * result + (clientKeyStoreName != null ? clientKeyStoreName.hashCode() : 0);
        result = hashcodePrimeNumber * result + (clientKeyStorePassword != null ? clientKeyStorePassword.hashCode() : 0);
        result = hashcodePrimeNumber * result + (clientKeyStoreType != null ? clientKeyStoreType.hashCode() : 0);

        result = hashcodePrimeNumber * result + (trustStoreName != null ? trustStoreName.hashCode() : 0);
        result = hashcodePrimeNumber * result + (trustStorePassword != null ? trustStorePassword.hashCode() : 0);
        result = hashcodePrimeNumber * result + (trustStoreType != null ? trustStoreType.hashCode() : 0);
        result = hashcodePrimeNumber * result + (trustManagerAlgorithm != null ? trustManagerAlgorithm.hashCode() : 0);
        result = hashcodePrimeNumber * result + (trustManagerFactory != null ? trustManagerFactory.hashCode() : 0);
        result = hashcodePrimeNumber * result + (explicitTrustStoreOnly ? 1 : 0);

        result = hashcodePrimeNumber * result + (requireClientAuthentication ? 1 : 0);
        result = hashcodePrimeNumber * result + (tlsProperties != null ? tlsProperties.hashCode() : 0);

        result = hashcodePrimeNumber * result + (rcStandardEnable != null ? rcStandardEnable.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcStandardOnlyEndEntities != null ? rcStandardOnlyEndEntities.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcStandardPreferCrls != null ? rcStandardPreferCrls.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcStandardNoFallback != null ? rcStandardNoFallback.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcStandardSoftFail != null ? rcStandardSoftFail.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcCrlFilePath != null ? rcCrlFilePath.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcCustomOcspUrl != null ? rcCustomOcspUrl.hashCode() : 0);
        result = hashcodePrimeNumber * result + (rcCustomOcspCertAlias != null ? rcCustomOcspCertAlias.hashCode() : 0);

        return result;
    }
}

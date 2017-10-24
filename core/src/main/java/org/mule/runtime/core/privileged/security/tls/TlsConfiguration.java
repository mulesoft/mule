/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.security.tls;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.privileged.security.TlsDirectKeyStore;
import org.mule.runtime.core.privileged.security.TlsDirectTrustStore;
import org.mule.runtime.core.privileged.security.TlsIndirectKeyStore;
import org.mule.runtime.core.api.util.FileUtils;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.secutiry.tls.RestrictedSSLServerSocketFactory;
import org.mule.runtime.core.internal.secutiry.tls.RestrictedSSLSocketFactory;
import org.mule.runtime.core.internal.secutiry.tls.TlsProperties;
import org.mule.runtime.core.internal.secutiry.tls.TlsPropertiesMapper;
import org.mule.runtime.core.internal.secutiry.tls.TlsPropertiesSocketFactory;
import org.mule.runtime.core.internal.util.ArrayUtils;
import org.mule.runtime.core.internal.util.SecurityUtils;
import org.mule.runtime.core.privileged.security.TlsIndirectTrustStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for configuring TLS/SSL connections.
 * <p/>
 * <h2>Introduction</h2>
 * <p/>
 * This class was introduced to centralise the work of TLS/SSL configuration. It is intended to be backwards compatible with
 * earlier code (as much as possible) and so is perhaps more complex than would be necessary if starting from zero - the main
 * source of confusion is the distinction between direct and indirect creation of sockets and stores.
 * <p/>
 * <h2>Configuration</h2>
 * <p/>
 * The documentation in this class is intended more for programmers than end uses. If you are configuring a connector the
 * interfaces {@link TlsIndirectTrustStore}, {@link TlsDirectTrustStore},
 * {@link TlsDirectKeyStore} and {@link TlsIndirectKeyStore} should provide guidance to individual properties. In addition you
 * should check the documentation for the specific protocol / connector used and may also need to read the discussion on direct
 * and indirect socket and store creation below (or, more simply, just use whichever key store interface your connector
 * implements!).
 * <p/>
 * <h2>Programming</h2>
 * <p/>
 * This class is intended to be used as a delegate as we typically want to add security to an already existing connector (so we
 * inherit from that connector, implement the appropriate interfaces from
 * {@link TlsIndirectTrustStore}, {@link TlsDirectTrustStore}, {@link TlsDirectKeyStore} and
 * {@link TlsIndirectKeyStore}, and then forward calls to the interfaces to an instance of this class).
 * <p/>
 * <p>
 * For setting System properties (and reading them) use {@link TlsPropertiesMapper}. This can take a "namespace" which can then be
 * used by {@link TlsPropertiesSocketFactory} to construct an appropriate socket factory. This approach (storing to properties and
 * then retrieving that information later in a socket factory) lets us pass TLS/SSL configuration into libraries that are
 * configured by specifying on the socket factory class.
 * </p>
 * <p/>
 * <h2>Direct and Indirect Socket and Store Creation</h2>
 * <p/>
 * For the SSL transport, which historically defined parameters for many different secure transports, the configuration interfaces
 * worked as follows:
 * <p/>
 * <dl>
 * <dt>{@link TlsDirectTrustStore}</dt>
 * <dd>Used to generate trust store directly and indirectly for all TLS/SSL conections via System properties</dd>
 * <dt>{@link TlsDirectKeyStore}</dt>
 * <dd>Used to generate key store directly</dd>
 * <dt>{@link TlsIndirectKeyStore}</dt>
 * <dd>Used to generate key store indirectly for all TLS/SSL conections via System properties</dd>
 * </dl>
 * <p/>
 * Historically, many other transports relied on the indirect configurations defined above. So they implemented
 * {@link TlsIndirectTrustStore} (a superclass of {@link TlsDirectTrustStore}) and relied on
 * {@link TlsIndirectKeyStore} from the SSL configuration. For continuity these interfaces continue to be used, even though the
 * configurations are now typically (see individual connector/protocol documentation) specific to a protocol or connector.
 * <em>Note - these interfaces are new, but the original code had those methods, used as described. The new interfaces only make
 * things explicit.</em>
 * <p/>
 * <p>
 * <em>Note for programmers</em> One way to understand the above is to see that many protocols are handled by libraries that are
 * configured by providing either properties or a socket factory. In both cases (the latter via
 * {@link TlsPropertiesSocketFactory}) we continue to use properties and the "indirect" interface. Note also that the mapping in
 * {@link TlsPropertiesMapper} correctly handles the asymmetry, so an initial call to {@link TlsConfiguration} uses the keystore
 * defined via {@link TlsDirectKeyStore}, but when a {@link TlsConfiguration} is retrieved from System proerties using
 * {@link TlsPropertiesMapper#readFromProperties(TlsConfiguration,java.util.Properties)} the "indirect" properties are supplied as
 * "direct" values, meaning that the "indirect" socket factory can be retrieved from {@link #getKeyManagerFactory()}. It just
 * works.
 * </p>
 */
public final class TlsConfiguration extends AbstractComponent
    implements TlsDirectTrustStore, TlsDirectKeyStore, TlsIndirectKeyStore {

  public static final String DEFAULT_KEYSTORE = ".keystore";
  public static final String DEFAULT_KEYSTORE_TYPE = KeyStore.getDefaultType();
  public static final String DEFAULT_KEYMANAGER_ALGORITHM = KeyManagerFactory.getDefaultAlgorithm();
  public static final String DEFAULT_SSL_TYPE = "TLS";
  public static final String JSSE_NAMESPACE = "javax.net";

  public static final String PROPERTIES_FILE_PATTERN = "tls-%s.conf";
  public static final String DEFAULT_SECURITY_MODEL = "default";

  private Logger logger = LoggerFactory.getLogger(getClass());

  private String sslType;

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

  /**
   * Support for TLS connections with a given initial value for the key store
   *
   * @param keyStore initial value for the key store
   */
  public TlsConfiguration(String keyStore) {
    this.keyStoreName = keyStore;
  }

  // note - in what follows i'm using "raw" variables rather than accessors because
  // i think the names are clearer. the API names for the accessors are historical
  // and not a close fit to actual use (imho).

  /**
   * @param anon If the connection is anonymous then we don't care about client keys
   * @param namespace Namespace to use for global properties (for JSSE use JSSE_NAMESPACE)
   * @throws CreateException ON initialisation problems
   */
  public void initialise(boolean anon, String namespace) throws CreateException {
    if (logger.isDebugEnabled()) {
      logger.debug("initialising: anon " + anon);
    }
    validate(anon);


    if (!anon) {
      initKeyManagerFactory();
    }
    initTrustManagerFactory();

    tlsProperties.load(String.format(PROPERTIES_FILE_PATTERN, SecurityUtils.getSecurityModel()));

    if (sslType == null) {
      sslType = resolveSslType();
    }
  }

  private String resolveSslType() {
    if (tlsProperties.getDefaultProtocol() != null) {
      return tlsProperties.getDefaultProtocol();
    } else {
      return DEFAULT_SSL_TYPE;
    }
  }

  private void validate(boolean anon) throws CreateException {
    if (!anon) {
      assertNotNull(getKeyStore(), "The KeyStore location cannot be null");
      assertNotNull(getKeyPassword(), "The Key password cannot be null");
      assertNotNull(getKeyStorePassword(), "The KeyStore password cannot be null");
      assertNotNull(getKeyManagerAlgorithm(), "The Key Manager Algorithm cannot be null");
    }
  }

  private void initKeyManagerFactory() throws CreateException {
    if (logger.isDebugEnabled()) {
      logger.debug("initialising key manager factory from keystore data");
    }

    KeyStore tempKeyStore;
    try {
      tempKeyStore = loadKeyStore();
      checkKeyStoreContainsAlias(tempKeyStore);
    } catch (Exception e) {
      throw new CreateException(CoreMessages.failedToLoad("KeyStore: " + keyStoreName), e, this);
    }

    try {
      keyManagerFactory = KeyManagerFactory.getInstance(getKeyManagerAlgorithm());
      keyManagerFactory.init(tempKeyStore, keyPassword.toCharArray());
    } catch (Exception e) {
      throw new CreateException(CoreMessages.failedToLoad("Key Manager"), e, this);
    }
  }

  private KeyStore loadKeyStore() throws GeneralSecurityException, IOException {
    KeyStore tempKeyStore = KeyStore.getInstance(keystoreType);

    InputStream is = IOUtils.getResourceAsStream(keyStoreName, getClass());
    if (null == is) {
      throw new FileNotFoundException(CoreMessages.cannotLoadFromClasspath("Keystore: " + keyStoreName).getMessage());
    }

    tempKeyStore.load(is, keyStorePassword.toCharArray());
    return tempKeyStore;
  }

  private void checkKeyStoreContainsAlias(KeyStore keyStore) throws KeyStoreException {
    if (!isBlank(keyAlias)) {
      boolean keyAliasFound = false;

      Enumeration<String> aliases = keyStore.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();

        if (alias.equals(keyAlias)) {
          // if alias is found all is valid but continue processing to strip out all
          // other (unwanted) keys
          keyAliasFound = true;
        } else {
          // if the current alias is not the one we are looking for, remove
          // it from the keystore
          keyStore.deleteEntry(alias);
        }
      }

      // if the alias was not found, throw an exception
      if (!keyAliasFound) {
        throw new IllegalStateException("Key with alias \"" + keyAlias + "\" was not found");
      }
    }
  }

  private void initTrustManagerFactory() throws CreateException {
    if (null != trustStoreName) {
      trustStorePassword = null == trustStorePassword ? "" : trustStorePassword;

      KeyStore trustStore;
      try {
        trustStore = KeyStore.getInstance(trustStoreType);
        InputStream is = IOUtils.getResourceAsStream(trustStoreName, getClass());
        if (null == is) {
          throw new FileNotFoundException("Failed to load truststore from classpath or local file: " + trustStoreName);
        }
        trustStore.load(is, trustStorePassword.toCharArray());
      } catch (Exception e) {
        throw new CreateException(CoreMessages.failedToLoad("TrustStore: " + trustStoreName), e, this);
      }

      try {
        trustManagerFactory = TrustManagerFactory.getInstance(trustManagerAlgorithm);
        trustManagerFactory.init(trustStore);
      } catch (Exception e) {
        throw new CreateException(CoreMessages.failedToLoad("Trust Manager (" + trustManagerAlgorithm + ")"), e, this);
      }
    }
  }


  private static void assertNotNull(Object value, String message) {
    if (null == value) {
      throw new IllegalArgumentException(message);
    }
  }

  private static String defaultForNull(String value, String deflt) {
    if (null == value) {
      return deflt;
    } else {
      return value;
    }
  }

  public SSLSocketFactory getSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
    return new RestrictedSSLSocketFactory(getSslContext(), getEnabledCipherSuites(), getEnabledProtocols());
  }

  public SSLServerSocketFactory getServerSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
    return new RestrictedSSLServerSocketFactory(getSslContext(), getEnabledCipherSuites(), getEnabledProtocols());
  }


  public String[] getEnabledCipherSuites() {
    return tlsProperties.getEnabledCipherSuites();
  }

  public String[] getEnabledProtocols() {
    return tlsProperties.getEnabledProtocols();
  }

  public SSLContext getSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    TrustManager[] trustManagers = null == getTrustManagerFactory() ? null : getTrustManagerFactory().getTrustManagers();

    return getSslContext(trustManagers);
  }

  public SSLContext getSslContext(TrustManager[] trustManagers) throws NoSuchAlgorithmException, KeyManagementException {
    KeyManager[] keyManagers = null == getKeyManagerFactory() ? null : getKeyManagerFactory().getKeyManagers();

    SSLContext context = SSLContext.getInstance(getSslType());
    // TODO - nice to have a configurable random number source set here
    context.init(keyManagers, trustManagers, null);
    return context;
  }

  public String getSslType() {
    return sslType;
  }

  public void setSslType(String sslType) {
    String[] enabledProtocols = tlsProperties.getEnabledProtocols();

    if (enabledProtocols != null && !ArrayUtils.contains(enabledProtocols, sslType)) {
      throw new IllegalArgumentException(String.format("Protocol %s is not allowed in current configuration", sslType));
    }

    this.sslType = sslType;
  }

  // access to the explicit key store variables

  @Override
  public String getKeyStore() {
    return keyStoreName;
  }

  @Override
  public void setKeyStore(String name) throws IOException {
    keyStoreName = name;
    if (null != keyStoreName) {
      keyStoreName = FileUtils.getResourcePath(keyStoreName, getClass());
      if (logger.isDebugEnabled()) {
        logger.debug("Normalised keyStore path to: " + keyStoreName);
      }
    }
  }

  @Override
  public String getKeyPassword() {
    return keyPassword;
  }

  @Override
  public void setKeyPassword(String keyPassword) {
    this.keyPassword = keyPassword;
  }

  @Override
  public String getKeyStorePassword() {
    return keyStorePassword;
  }

  @Override
  public void setKeyStorePassword(String storePassword) {
    this.keyStorePassword = storePassword;
  }

  @Override
  public String getKeyStoreType() {
    return keystoreType;
  }

  @Override
  public void setKeyStoreType(String keystoreType) {
    this.keystoreType = keystoreType;
  }

  @Override
  public String getKeyManagerAlgorithm() {
    return keyManagerAlgorithm;
  }

  @Override
  public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
    this.keyManagerAlgorithm = keyManagerAlgorithm;
  }

  @Override
  public KeyManagerFactory getKeyManagerFactory() {
    return keyManagerFactory;
  }

  // access to the implicit key store variables

  @Override
  public String getClientKeyStore() {
    return clientKeyStoreName;
  }

  @Override
  public void setClientKeyStore(String name) throws IOException {
    clientKeyStoreName = name;
    if (null != clientKeyStoreName) {
      clientKeyStoreName = FileUtils.getResourcePath(clientKeyStoreName, getClass());
      if (logger.isDebugEnabled()) {
        logger.debug("Normalised clientKeyStore path to: " + clientKeyStoreName);
      }
    }
  }

  @Override
  public String getClientKeyStorePassword() {
    return clientKeyStorePassword;
  }

  @Override
  public void setClientKeyStorePassword(String clientKeyStorePassword) {
    this.clientKeyStorePassword = clientKeyStorePassword;
  }

  @Override
  public void setClientKeyStoreType(String clientKeyStoreType) {
    this.clientKeyStoreType = clientKeyStoreType;
  }

  @Override
  public String getClientKeyStoreType() {
    return clientKeyStoreType;
  }

  // access to trust store variables

  @Override
  public String getTrustStore() {
    return trustStoreName;
  }

  @Override
  public void setTrustStore(String name) throws IOException {
    trustStoreName = name;
    if (null != trustStoreName) {
      trustStoreName = FileUtils.getResourcePath(trustStoreName, getClass());
      if (logger.isDebugEnabled()) {
        logger.debug("Normalised trustStore path to: " + trustStoreName);
      }
    }
  }

  @Override
  public String getTrustStorePassword() {
    return trustStorePassword;
  }

  @Override
  public void setTrustStorePassword(String trustStorePassword) {
    this.trustStorePassword = trustStorePassword;
  }

  @Override
  public String getTrustStoreType() {
    return trustStoreType;
  }

  @Override
  public void setTrustStoreType(String trustStoreType) {
    this.trustStoreType = trustStoreType;
  }

  @Override
  public String getTrustManagerAlgorithm() {
    return trustManagerAlgorithm;
  }

  @Override
  public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
    this.trustManagerAlgorithm = defaultForNull(trustManagerAlgorithm, DEFAULT_KEYMANAGER_ALGORITHM);
  }

  @Override
  public TrustManagerFactory getTrustManagerFactory() {
    return trustManagerFactory;
  }

  @Override
  public void setTrustManagerFactory(TrustManagerFactory trustManagerFactory) {
    this.trustManagerFactory = trustManagerFactory;
  }

  @Override
  public boolean isExplicitTrustStoreOnly() {
    return explicitTrustStoreOnly;
  }

  @Override
  public void setExplicitTrustStoreOnly(boolean explicitTrustStoreOnly) {
    this.explicitTrustStoreOnly = explicitTrustStoreOnly;
  }

  @Override
  public boolean isRequireClientAuthentication() {
    return requireClientAuthentication;
  }

  @Override
  public void setRequireClientAuthentication(boolean requireClientAuthentication) {
    this.requireClientAuthentication = requireClientAuthentication;
  }

  @Override
  public String getKeyAlias() {
    return keyAlias;
  }

  @Override
  public void setKeyAlias(String keyAlias) {
    this.keyAlias = keyAlias;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TlsConfiguration)) {
      return false;
    }

    TlsConfiguration that = (TlsConfiguration) o;

    if (explicitTrustStoreOnly != that.explicitTrustStoreOnly) {
      return false;
    }
    if (requireClientAuthentication != that.requireClientAuthentication) {
      return false;
    }
    if (clientKeyStoreName != null ? !clientKeyStoreName.equals(that.clientKeyStoreName) : that.clientKeyStoreName != null) {
      return false;
    }
    if (clientKeyStorePassword != null ? !clientKeyStorePassword.equals(that.clientKeyStorePassword)
        : that.clientKeyStorePassword != null) {
      return false;
    }
    if (clientKeyStoreType != null ? !clientKeyStoreType.equals(that.clientKeyStoreType) : that.clientKeyStoreType != null) {
      return false;
    }
    if (keyAlias != null ? !keyAlias.equals(that.keyAlias) : that.keyAlias != null) {
      return false;
    }
    if (keyManagerAlgorithm != null ? !keyManagerAlgorithm.equals(that.keyManagerAlgorithm) : that.keyManagerAlgorithm != null) {
      return false;
    }
    if (keyManagerFactory != null ? !keyManagerFactory.equals(that.keyManagerFactory) : that.keyManagerFactory != null) {
      return false;
    }
    if (keyPassword != null ? !keyPassword.equals(that.keyPassword) : that.keyPassword != null) {
      return false;
    }
    if (keyStoreName != null ? !keyStoreName.equals(that.keyStoreName) : that.keyStoreName != null) {
      return false;
    }
    if (keyStorePassword != null ? !keyStorePassword.equals(that.keyStorePassword) : that.keyStorePassword != null) {
      return false;
    }
    if (keystoreType != null ? !keystoreType.equals(that.keystoreType) : that.keystoreType != null) {
      return false;
    }
    if (sslType != null ? !sslType.equals(that.sslType) : that.sslType != null) {
      return false;
    }
    if (tlsProperties != null ? !tlsProperties.equals(that.tlsProperties) : that.tlsProperties != null) {
      return false;
    }
    if (trustManagerAlgorithm != null ? !trustManagerAlgorithm.equals(that.trustManagerAlgorithm)
        : that.trustManagerAlgorithm != null) {
      return false;
    }
    if (trustManagerFactory != null ? !trustManagerFactory.equals(that.trustManagerFactory) : that.trustManagerFactory != null) {
      return false;
    }
    if (trustStoreName != null ? !trustStoreName.equals(that.trustStoreName) : that.trustStoreName != null) {
      return false;
    }
    if (trustStorePassword != null ? !trustStorePassword.equals(that.trustStorePassword) : that.trustStorePassword != null) {
      return false;
    }
    if (trustStoreType != null ? !trustStoreType.equals(that.trustStoreType) : that.trustStoreType != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = sslType != null ? sslType.hashCode() : 0;
    int hashcodePrimeNumber = 31;
    result = hashcodePrimeNumber * result + (keyStoreName != null ? keyStoreName.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keyAlias != null ? keyAlias.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keyPassword != null ? keyPassword.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keyStorePassword != null ? keyStorePassword.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keystoreType != null ? keystoreType.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keyManagerAlgorithm != null ? keyManagerAlgorithm.hashCode() : 0);
    result = hashcodePrimeNumber * result + (keyManagerFactory != null ? keyManagerFactory.hashCode() : 0);
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
    return result;
  }
}



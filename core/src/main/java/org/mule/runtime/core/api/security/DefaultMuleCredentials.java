/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.security.Credentials;

import java.io.Serializable;
import java.util.StringTokenizer;

import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * Default implementation of {@link Credentials}.
 * {@code DefaultMuleCredentials} can be used to read and set Mule user information that can be stored in a message header.
 *
 * @since 4.0
 */
public class DefaultMuleCredentials implements Credentials, Serializable {

  private static final String TOKEN_DELIM = "::";

  private final String username;
  private final char[] password;
  private Object roles;

  public DefaultMuleCredentials(String username, char[] password) {
    this.username = username;
    this.password = ArrayUtils.clone(password);
  }

  public DefaultMuleCredentials(String username, char[] password, Object roles) {
    this.username = username;
    this.password = ArrayUtils.clone(password);
    this.roles = roles;
  }

  public DefaultMuleCredentials(String header, SecurityManager sm)
      throws EncryptionStrategyNotFoundException, CryptoFailureException {

    int i = header.indexOf(' ');
    if (i == -1) {
      throw new IllegalArgumentException(
                                         createStaticMessage("Header field 'MULE_USER' is malformed. Value is '%s'", header)
                                             .toString());
    }

    String scheme = header.substring(0, i);
    String creds = header.substring(i + 1);

    if (!scheme.equalsIgnoreCase("plain")) {
      EncryptionStrategy es = sm.getEncryptionStrategy(scheme);
      if (es == null) {
        throw new EncryptionStrategyNotFoundException(scheme);
      } else {
        creds = new String(es.decrypt(creds.getBytes(), null));
      }
    }

    StringTokenizer st = new StringTokenizer(creds, TOKEN_DELIM);
    username = st.nextToken();
    password = st.nextToken().toCharArray();
    if (st.hasMoreTokens()) {
      roles = st.nextToken();
    }
  }

  public String getToken() {
    StringBuilder buf = new StringBuilder();
    buf.append(username).append(TOKEN_DELIM);
    buf.append(password).append(TOKEN_DELIM);

    if (roles != null) {
      buf.append(roles);
    }

    return buf.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUsername() {
    return username;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public char[] getPassword() {
    return ArrayUtils.clone(password);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getRoles() {
    return roles;
  }

  public static String createHeader(String username, char[] password) {
    StringBuilder buf = new StringBuilder(32);
    buf.append("Plain ");
    buf.append(username).append(TOKEN_DELIM);
    buf.append(password).append(TOKEN_DELIM);
    return buf.toString();
  }

  public static String createHeader(String username, String password, String encryptionName, EncryptionStrategy es)
      throws CryptoFailureException {
    StringBuilder buf = new StringBuilder();
    buf.append(encryptionName).append(" ");
    String creds = username + TOKEN_DELIM + password;
    byte[] encrypted = es.encrypt(creds.getBytes(), null);
    buf.append(new String(encrypted));
    return buf.toString();
  }
}

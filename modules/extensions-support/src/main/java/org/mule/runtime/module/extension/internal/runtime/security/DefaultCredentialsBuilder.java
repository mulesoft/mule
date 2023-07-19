/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.security;

import org.mule.runtime.api.security.Credentials;
import org.mule.runtime.api.security.CredentialsBuilder;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;

/**
 * Builder for a {@link Credentials} implementation.
 *
 * @since 1.0
 */
public final class DefaultCredentialsBuilder implements CredentialsBuilder {

  private String username;
  private char[] password;
  private Object roles;

  DefaultCredentialsBuilder() {}

  /**
   * @param username the username of this {@link Credentials}
   * @return {@code this} builder
   */
  public DefaultCredentialsBuilder withUsername(String username) {
    this.username = username;
    return this;
  }

  /**
   * @param password the password of this {@link Credentials}
   * @return {@code this} builder
   */
  public DefaultCredentialsBuilder withPassword(char[] password) {
    this.password = password;
    return this;
  }

  /**
   * @param roles the enabled roles of this {@link Credentials}
   * @return {@code this} builder
   */
  public DefaultCredentialsBuilder withRoles(Object roles) {
    this.roles = roles;
    return this;
  }

  /**
   * @return an instance of a default implementation of {@link Credentials}
   */
  public Credentials build() {
    return new DefaultMuleCredentials(username, password, roles);
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.core.api.security.DefaultMuleCredentials.createHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class DefaultMuleCredentialsTestCase {

  @Test
  public void testConstructorWithUsernameAndPassword() {
    char[] password = {'p', 'a', 's', 's'};
    DefaultMuleCredentials credentials = new DefaultMuleCredentials("user", password);

    assertThat(credentials.getUsername(), is("user"));
    assertThat(credentials.getPassword(), is(not(sameInstance(password))));
    assertThat(credentials.getPassword(), is(password));
    assertThat(credentials.getRoles(), is(nullValue()));
  }

  @Test
  public void testConstructorWithUsernamePasswordAndRoles() {
    char[] password = {'p', 'a', 's', 's'};
    DefaultMuleCredentials credentials = new DefaultMuleCredentials("user", password, "admin");

    assertThat(credentials.getUsername(), is("user"));
    assertThat(credentials.getPassword(), is(not(sameInstance(password))));
    assertThat(credentials.getPassword(), is(password));
    assertThat(credentials.getRoles(), is("admin"));
  }

  @Test
  public void testGetToken() {
    char[] password = {'p', 'a', 's', 's'};
    DefaultMuleCredentials credentials = new DefaultMuleCredentials("user", password, "admin");

    assertThat(credentials.getToken(), is("user::pass::admin"));
  }

  @Test
  public void testGetTokenNoRoles() {
    char[] password = {'p', 'a', 's', 's'};
    DefaultMuleCredentials credentials = new DefaultMuleCredentials("user", password, null);

    assertThat(credentials.getToken(), is("user::pass::"));
  }

  @Test
  public void testPasswordCloning() {
    char[] password = {'s', 'e', 'c', 'r', 'e', 't'};
    DefaultMuleCredentials credentials = new DefaultMuleCredentials("user", password);

    password[0] = 'x';

    assertThat(credentials.getPassword(), is(not(password)));
    assertThat(credentials.getPassword(), is(new char[] {'s', 'e', 'c', 'r', 'e', 't'}));
  }

  @Test
  public void testCreateHeader() {
    char[] password = {'p', 'a', 's', 's'};
    String header = createHeader("user", password);

    assertThat(header, is("Plain user::pass::"));
  }

  @Test
  public void testCreateHeaderWithEncryption() throws Exception {
    EncryptionStrategy encryptionStrategy = mock(EncryptionStrategy.class);
    when(encryptionStrategy.encrypt(any(byte[].class), isNull()))
        .thenReturn("encryptedData".getBytes());

    String header = createHeader("user", "password", "AES", encryptionStrategy);

    assertThat(header, is("AES encryptedData"));
  }

  @Test
  public void testConstructorWithPlainHeader() throws Exception {
    String header = "plain user::password::admin";
    SecurityManager securityManager = mock(SecurityManager.class);

    DefaultMuleCredentials credentials = new DefaultMuleCredentials(header, securityManager);

    assertThat(credentials.getUsername(), is("user"));
    assertThat(credentials.getPassword(), is("password".toCharArray()));
    assertThat(credentials.getRoles(), is("admin"));
  }

  @Test
  public void testConstructorWithEncryptedHeader() throws Exception {
    String encryptedData = "encryptedData";
    String decryptedData = "user::password";
    String header = "AES " + encryptedData;

    EncryptionStrategy encryptionStrategy = mock(EncryptionStrategy.class);
    when(encryptionStrategy.decrypt(eq(encryptedData.getBytes()), isNull()))
        .thenReturn(decryptedData.getBytes());

    SecurityManager securityManager = mock(SecurityManager.class);
    when(securityManager.getEncryptionStrategy("AES")).thenReturn(encryptionStrategy);

    DefaultMuleCredentials credentials = new DefaultMuleCredentials(header, securityManager);

    assertThat(credentials.getUsername(), is("user"));
    assertThat(credentials.getPassword(), is("password".toCharArray()));
    assertThat(credentials.getRoles(), is(nullValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithMalformedHeader() throws Exception {
    String header = "invalidHeaderWithoutSpace";
    SecurityManager securityManager = mock(SecurityManager.class);

    new DefaultMuleCredentials(header, securityManager);
  }

  @Test(expected = EncryptionStrategyNotFoundException.class)
  public void testConstructorWithMissingEncryptionStrategy() throws Exception {
    String header = "AES encryptedData";
    SecurityManager securityManager = mock(SecurityManager.class);
    when(securityManager.getEncryptionStrategy("AES")).thenReturn(null);

    new DefaultMuleCredentials(header, securityManager);
  }
}

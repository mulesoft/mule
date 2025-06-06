/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.http.api.client.auth.HttpAuthentication;

import org.junit.jupiter.api.Test;

class HttpAuthenticationConfigurerImplTest {

  private final HttpAuthenticationConfigurerImpl configurer = new HttpAuthenticationConfigurerImpl();

  @Test
  void testBasicAuthenticationWithPreemptive() {
    configurer.basic("user1", "pass1", true);
    HttpAuthentication auth = configurer.build();

    assertThat(auth, notNullValue());
    assertThat(auth.getType().name(), is("BASIC"));
    assertThat(auth.getUsername(), is("user1"));
    assertThat(auth.getPassword(), is("pass1"));
    assertThat(auth.isPreemptive(), is(true));
  }

  @Test
  void testBasicAuthenticationWithoutPreemptive() {
    configurer.basic("user2", "pass2", false);
    HttpAuthentication auth = configurer.build();

    assertThat(auth, notNullValue());
    assertThat(auth.getType().name(), is("BASIC"));
    assertThat(auth.getUsername(), is("user2"));
    assertThat(auth.getPassword(), is("pass2"));
    assertThat(auth.isPreemptive(), is(false));
  }

  @Test
  void testDigestAuthenticationWithPreemptive() {
    configurer.digest("user1", "pass1", true);
    HttpAuthentication auth = configurer.build();

    assertThat(auth, notNullValue());
    assertThat(auth.getType().name(), is("DIGEST"));
    assertThat(auth.getUsername(), is("user1"));
    assertThat(auth.getPassword(), is("pass1"));
    assertThat(auth.isPreemptive(), is(true));
  }

  @Test
  void testDigestAuthenticationWithoutPreemptive() {
    configurer.digest("user2", "pass2", false);
    HttpAuthentication auth = configurer.build();

    assertThat(auth, notNullValue());
    assertThat(auth.getType().name(), is("DIGEST"));
    assertThat(auth.getUsername(), is("user2"));
    assertThat(auth.getPassword(), is("pass2"));
    assertThat(auth.isPreemptive(), is(false));
  }

  @Test
  void testNtlmAuthenticationWithAllParameters() {
    configurer.ntlm("user1", "pass1", true, "domain", "workstation");
    HttpAuthentication auth = configurer.build();

    assertThat(auth, instanceOf(HttpAuthentication.HttpNtlmAuthentication.class));
    HttpAuthentication.HttpNtlmAuthentication ntlmAuth = (HttpAuthentication.HttpNtlmAuthentication) auth;
    assertThat(ntlmAuth.getType().name(), is("NTLM"));
    assertThat(ntlmAuth.getUsername(), is("user1"));
    assertThat(ntlmAuth.getPassword(), is("pass1"));
    assertThat(ntlmAuth.getDomain(), is("domain"));
    assertThat(ntlmAuth.getWorkstation(), is("workstation"));
    assertThat(ntlmAuth.isPreemptive(), is(true));
  }

  @Test
  void testNtlmAuthenticationWithMinimalParameters() {
    configurer.ntlm("user2", "pass2", false, null, null);
    HttpAuthentication auth = configurer.build();

    assertThat(auth, instanceOf(HttpAuthentication.HttpNtlmAuthentication.class));
    HttpAuthentication.HttpNtlmAuthentication ntlmAuth = (HttpAuthentication.HttpNtlmAuthentication) auth;
    assertThat(ntlmAuth.getType().name(), is("NTLM"));
    assertThat(ntlmAuth.getUsername(), is("user2"));
    assertThat(ntlmAuth.getPassword(), is("pass2"));
    assertThat(ntlmAuth.getDomain(), is(nullValue()));
    assertThat(ntlmAuth.getWorkstation(), is(nullValue()));
    assertThat(ntlmAuth.isPreemptive(), is(false));
  }
}

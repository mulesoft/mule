/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.client.auth;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.BASIC;
import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.DIGEST;
import static org.mule.runtime.http.api.client.auth.HttpAuthenticationType.NTLM;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

import org.mule.runtime.http.api.client.auth.HttpAuthentication.HttpNtlmAuthentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(HTTP_SERVICE)
@Story("Authentication Builder")
public class HttpAuthenticationBuilderTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void builder() {
    HttpAuthentication auth = HttpAuthentication.builder().type(BASIC).username("Helena").password("s33stra").build();

    assertThat(auth.getType(), is(BASIC));
    assertThat(auth.getUsername(), is("Helena"));
    assertThat(auth.getPassword(), is("s33stra"));
    assertThat(auth.isPreemptive(), is(true));
  }

  @Test
  public void ntlmBuilder() {
    HttpNtlmAuthentication auth = HttpNtlmAuthentication.builder().username("Allison").password("suburbia").domain("CA").build();

    assertThat(auth.getType(), is(NTLM));
    assertThat(auth.getUsername(), is("Allison"));
    assertThat(auth.getPassword(), is("suburbia"));
    assertThat(auth.getDomain(), is("CA"));
    assertThat(auth.getWorkstation(), is(nullValue()));
  }

  @Test
  public void basic() {
    HttpAuthentication auth = HttpAuthentication.basic("Sarah", "l3D4").build();

    assertThat(auth.getType(), is(BASIC));
    assertThat(auth.getUsername(), is("Sarah"));
    assertThat(auth.getPassword(), is("l3D4"));
  }

  @Test
  public void digest() {
    HttpAuthentication auth = HttpAuthentication.digest("Cosima", "324B21").preemptive(false).build();

    assertThat(auth.getType(), is(DIGEST));
    assertThat(auth.getUsername(), is("Cosima"));
    assertThat(auth.getPassword(), is("324B21"));
    assertThat(auth.isPreemptive(), is(false));
  }

  @Test
  public void ntlm() {
    HttpNtlmAuthentication auth = HttpAuthentication.ntlm("Rachel", "Cold B Digest")
        .domain("DYAD")
        .workstation("MyPC")
        .build();

    assertThat(auth.getType(), is(NTLM));
    assertThat(auth.getUsername(), is("Rachel"));
    assertThat(auth.getPassword(), is("Cold B Digest"));
    assertThat(auth.isPreemptive(), is(true));
    assertThat(auth.getDomain(), is("DYAD"));
    assertThat(auth.getWorkstation(), is("MyPC"));
  }

  @Test
  public void failWithoutType() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("An authentication type must be declared.");
    HttpAuthentication.builder().username("Tony").password("W4T").build();
  }

  @Test
  public void failWithoutUsername() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A username must be provided.");
    HttpAuthentication.builder().type(DIGEST).password("C4s70r").build();
  }

  @Test
  public void failWithoutPassword() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("A password must be provided.");
    HttpNtlmAuthentication.builder().type(NTLM).username("Krystal").build();
  }

}

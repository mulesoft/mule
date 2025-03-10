/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.i18n.I18nMessage;

import org.junit.Test;

public class CryptoFailureExceptionTestCase {

  @Test
  public void testExceptionMessageWithStrategy() {
    EncryptionStrategy strategy = mock(EncryptionStrategy.class);
    when(strategy.toString()).thenReturn("MockStrategy");
    I18nMessage message = createStaticMessage("CryptoError");

    CryptoFailureException exception = new CryptoFailureException(message, strategy);

    assertThat(exception.getMessage(), containsString("CryptoError"));
    assertThat(exception.getEncryptionStrategy(), is(strategy));
    assertThat(exception.getInfo().get("Encryption"), is("MockStrategy"));
  }

  @Test
  public void testExceptionMessageWithStrategyAndCause() {
    EncryptionStrategy strategy = mock(EncryptionStrategy.class);
    when(strategy.toString()).thenReturn("MockStrategy");
    I18nMessage message = createStaticMessage("CryptoError");
    Throwable cause = new RuntimeException("failure");

    CryptoFailureException exception = new CryptoFailureException(message, strategy, cause);

    assertThat(exception.getMessage(), containsString("CryptoError"));
    assertThat(exception.getEncryptionStrategy(), is(strategy));
    assertThat(exception.getCause(), is(cause));
    assertThat(exception.getInfo().get("Encryption"), is("MockStrategy"));
  }

  @Test
  public void testExceptionMessageWithNullStrategy() {
    Throwable cause = new RuntimeException("failure");
    CryptoFailureException exception = new CryptoFailureException(null, cause);

    assertThat(exception.getMessage(), containsString("Crypto Failure"));
    assertThat(exception.getEncryptionStrategy(), is(nullValue()));
    assertThat(exception.getCause(), is(cause));
    assertThat(exception.getInfo().get("Encryption"), is("null"));
  }

  @Test
  public void testExceptionWithNullStrategyAndMessage() {
    I18nMessage message = createStaticMessage("Test Null Strategy");
    CryptoFailureException exception = new CryptoFailureException(message, null);

    assertThat(exception.getMessage(), containsString("Test Null Strategy"));
    assertThat(exception.getEncryptionStrategy(), is(nullValue()));
    assertThat(exception.getInfo().get("Encryption"), is("null"));
  }

  @Test
  public void testExceptionWithStrategyAndCause() {
    EncryptionStrategy strategy = mock(EncryptionStrategy.class);
    when(strategy.toString()).thenReturn("MockStrategy");
    Throwable cause = new RuntimeException("failure");

    CryptoFailureException exception = new CryptoFailureException(strategy, cause);

    assertThat(exception.getMessage(), containsString("Crypto Failure"));
    assertThat(exception.getEncryptionStrategy(), is(strategy));
    assertThat(exception.getCause(), is(cause));
    assertThat(exception.getInfo().get("Encryption"), is("MockStrategy"));
  }

  @Test
  public void testExceptionWithNullStrategyAndCause() {
    Throwable cause = new RuntimeException("failure");
    CryptoFailureException exception = new CryptoFailureException(null, cause);

    assertThat(exception.getMessage(), containsString("Crypto Failure"));
    assertThat(exception.getEncryptionStrategy(), is(nullValue()));
    assertThat(exception.getCause(), is(cause));
    assertThat(exception.getInfo().get("Encryption"), is("null"));
  }

  @Test
  public void testExceptionWithNullStrategyMessageAndCause() {
    I18nMessage message = createStaticMessage("Null Strategy with Cause");
    Throwable cause = new RuntimeException("failure");
    CryptoFailureException exception = new CryptoFailureException(message, null, cause);

    assertThat(exception.getMessage(), containsString("Null Strategy with Cause"));
    assertThat(exception.getEncryptionStrategy(), is(nullValue()));
    assertThat(exception.getCause(), is(cause));
    assertThat(exception.getInfo().get("Encryption"), is("null"));
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.core.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.exception.Errors.Identifiers.CRITICAL_IDENTIFIER;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.message.ErrorTypeBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifact;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TemporaryArtifactConnectivityTestingServiceTestCase extends AbstractMuleTestCase {

  private static final Location COMPONENT_LOCATION = builder().globalName("anyComponent").build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TemporaryArtifact mockTemporaryArtifact = mock(TemporaryArtifact.class, RETURNS_DEEP_STUBS);
  private TemporaryArtifactConnectivityTestingService connectivityTestingService;

  @Before
  public void createConnectivityService() {
    connectivityTestingService = new TemporaryArtifactConnectivityTestingService(mockTemporaryArtifact);
  }

  @Test(expected = IllegalArgumentException.class)
  public void componentIdentifierMustBeNotNull() throws Exception {
    connectivityTestingService.testConnection(null);
  }

  @Test
  public void initialisationExceptionDuringArtifactStartup() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(false);
    doThrow(InitialisationException.class).when(mockTemporaryArtifact).start();

    ConnectionValidationResult connectionResult = connectivityTestingService.testConnection(COMPONENT_LOCATION);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), instanceOf(InitialisationException.class));
  }

  @Test
  public void exceptionDuringArtifactStartup() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(false);
    doThrow(MuleRuntimeException.class).when(mockTemporaryArtifact).start();

    ConnectionValidationResult connectionResult = connectivityTestingService.testConnection(COMPONENT_LOCATION);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), instanceOf(MuleRuntimeException.class));
  }

  @Test
  public void connectionExceptionDuringArtifactStartup() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(false);
    doThrow(new RuntimeException(new ConnectionException("error", new RuntimeException(), ErrorTypeBuilder.builder()
        .namespace(CORE_NAMESPACE_NAME)
        .identifier(CRITICAL_IDENTIFIER)
        .build())))
            .when(mockTemporaryArtifact).start();

    ConnectionValidationResult connectionResult = connectivityTestingService.testConnection(COMPONENT_LOCATION);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), instanceOf(ConnectionException.class));
    assertThat(connectionResult.getErrorType().isPresent(), is(true));
    assertThat(connectionResult.getErrorType().get().getNamespace(), is(CORE_NAMESPACE_NAME));
    assertThat(connectionResult.getErrorType().get().getIdentifier(), is(CRITICAL_IDENTIFIER));
  }

}

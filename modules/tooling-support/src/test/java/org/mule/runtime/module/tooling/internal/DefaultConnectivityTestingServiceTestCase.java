/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.tooling.api.artifact.TemporaryArtifact;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingObjectNotFoundException;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.UnsupportedConnectivityTestingObjectException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultConnectivityTestingServiceTestCase extends AbstractMuleTestCase {

  private static final String COMPONENT_IDENTIFIER = "anyComponent";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TemporaryArtifact mockTemporaryArtifact = mock(TemporaryArtifact.class, RETURNS_DEEP_STUBS);
  private DefaultConnectivityTestingService connectivityTestingService;

  @Before
  public void createConnectivityService() {
    connectivityTestingService = new DefaultConnectivityTestingService(mockTemporaryArtifact);
  }

  @Test(expected = IllegalArgumentException.class)
  public void componentIdentifierMustBeNotNull() throws Exception {
    connectivityTestingService.testConnection(null);
  }

  @Test
  public void initialisationExceptionDuringArtifactStartup() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(false);
    doThrow(InitialisationException.class).when(mockTemporaryArtifact).start();

    ConnectionValidationResult connectionResult = connectivityTestingService.testConnection(COMPONENT_IDENTIFIER);
    assertThat(connectionResult.isValid(), is(false));
    assertThat(connectionResult.getException(), instanceOf(InitialisationException.class));
  }

  @Test
  public void exceptionDuringArtifactStartup() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(false);
    doThrow(MuleRuntimeException.class).when(mockTemporaryArtifact).start();

    expectedException.expect(MuleRuntimeException.class);
    connectivityTestingService.testConnection(COMPONENT_IDENTIFIER);
  }

  @Test
  public void noConnectivityObjectFound() throws MuleException {
    when(mockTemporaryArtifact.isStarted()).thenReturn(true);
    when(mockTemporaryArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class))
        .thenReturn(emptyList());

    expectedException.expect(UnsupportedConnectivityTestingObjectException.class);
    connectivityTestingService.testConnection(COMPONENT_IDENTIFIER);
  }

  @Test
  public void connectivityTestingObjectNotFound() throws Exception {
    when(mockTemporaryArtifact.isStarted()).thenReturn(true);
    when(mockTemporaryArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class))
        .thenReturn(emptyList());
    when(mockTemporaryArtifact.getMuleContext().getRegistry().get(COMPONENT_IDENTIFIER)).thenReturn(null);

    expectedException.expect(ConnectivityTestingObjectNotFoundException.class);
    connectivityTestingService.testConnection(COMPONENT_IDENTIFIER);
  }

}

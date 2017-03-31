/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connectivity;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.core.api.connectivity.UnsupportedConnectivityTestingObjectException;
import org.mule.runtime.core.api.exception.ObjectNotFoundException;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultConnectivityTestingServiceTestCase extends AbstractMuleTestCase {

  private static final String TEST_IDENTIFIER = "testIdentifier";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MuleContext mockMuleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
  private ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class, RETURNS_DEEP_STUBS);
  private ConnectivityTestingStrategy mockConnectivityTestingStrategy =
      mock(ConnectivityTestingStrategy.class, RETURNS_DEEP_STUBS);
  private DefaultConnectivityTestingService connectivityTestingService;
  private AnnotatedObject fakeConnectivityTestingObject = mock(AnnotatedObject.class);

  @Before
  public void createConnectivityService() throws InitialisationException {
    connectivityTestingService = new DefaultConnectivityTestingService();
    connectivityTestingService.setServiceRegistry(mockServiceRegistry);
    connectivityTestingService.setMuleContext(mockMuleContext);
    when(mockMuleContext.getConfigurationComponentLocator().find(any(Location.class)))
        .thenReturn(of(fakeConnectivityTestingObject));
    when(mockServiceRegistry.lookupProviders(any(), any())).thenReturn(asList(mockConnectivityTestingStrategy));
    when(mockConnectivityTestingStrategy.accepts(fakeConnectivityTestingObject)).thenReturn(true);
    when(mockMuleContext.getRegistry().get(TEST_IDENTIFIER)).thenReturn(fakeConnectivityTestingObject);
    connectivityTestingService.initialise();
  }

  @Test
  public void testConnectionThrowsException() throws Exception {
    RuntimeException exception = new RuntimeException();
    when(mockConnectivityTestingStrategy.testConnectivity(fakeConnectivityTestingObject)).thenThrow(exception);
    ConnectionValidationResult validationResult =
        connectivityTestingService.testConnection(builder().globalName(TEST_IDENTIFIER).build());

    assertThat(validationResult.isValid(), is(false));
    assertThat(validationResult.getException(), is(exception));
  }

  @Test
  public void testConnection() {
    when(mockConnectivityTestingStrategy.testConnectivity(fakeConnectivityTestingObject)).thenReturn(success());
    ConnectionValidationResult validationResult =
        connectivityTestingService.testConnection(builder().globalName(TEST_IDENTIFIER).build());
    assertThat(validationResult.isValid(), is(true));
  }

  @Test
  public void testObjectNotSupported() {
    reset(mockConnectivityTestingStrategy);
    when(mockConnectivityTestingStrategy.accepts(fakeConnectivityTestingObject)).thenReturn(false);
    expectedException.expect(UnsupportedConnectivityTestingObjectException.class);
    connectivityTestingService.testConnection(builder().globalName(TEST_IDENTIFIER).build());
  }

  @Test
  public void nonExistentConnectivityTestingObject() {
    reset(mockMuleContext);
    when(mockMuleContext.getConfigurationComponentLocator().find(any(Location.class))).thenReturn(empty());
    expectedException.expect(ObjectNotFoundException.class);
    connectivityTestingService.testConnection(builder().globalName(TEST_IDENTIFIER).build());
  }

}

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
import static org.mule.runtime.module.tooling.api.connectivity.ConnectionResult.Status.FAILURE;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifact;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.runtime.module.tooling.api.connectivity.NoConnectivityTestingObjectFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultConnectivityTestingServiceTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ToolingArtifact mockToolingArtifact = mock(ToolingArtifact.class, RETURNS_DEEP_STUBS);
    private DefaultConnectivityTestingService connectivityTestingService;

    @Before
    public void createConnectivityService()
    {
        connectivityTestingService = new DefaultConnectivityTestingService(mockToolingArtifact);
    }

    @Test
    public void initialisationExceptionDuringArtifactStartup() throws MuleException
    {
        when(mockToolingArtifact.isStarted()).thenReturn(false);
        doThrow(InitialisationException.class).when(mockToolingArtifact).start();

        ConnectionResult connectionResult = connectivityTestingService.testConnection();
        assertThat(connectionResult.getStatus(), is(FAILURE));
        assertThat(connectionResult.getException().get(), instanceOf(InitialisationException.class));
    }

    @Test
    public void exceptionDuringArtifactStartup() throws MuleException
    {
        when(mockToolingArtifact.isStarted()).thenReturn(false);
        doThrow(MuleRuntimeException.class).when(mockToolingArtifact).start();

        expectedException.expect(MuleRuntimeException.class);
        connectivityTestingService.testConnection();
    }

    @Test
    public void noConnectivityTestingObjectFound() throws MuleException
    {
        when(mockToolingArtifact.isStarted()).thenReturn(true);
        when(mockToolingArtifact.getMuleContext().getRegistry().lookupObjects(ConnectivityTestingStrategy.class)).thenReturn(emptyList());

        expectedException.expect(NoConnectivityTestingObjectFoundException.class);
        connectivityTestingService.testConnection();
    }

}
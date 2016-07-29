/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.module.launcher.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.launcher.TemporaryToolingArtifactBuilderFactory;
import org.mule.runtime.module.repository.api.BundleNotFoundException;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectionResult;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.module.tooling.api.connectivity.MultipleConnectivityTestingObjectsFoundException;
import org.mule.runtime.module.tooling.api.connectivity.NoConnectivityTestingObjectFoundException;
import org.mule.runtime.module.tooling.internal.DefaultToolingService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class ToolingServiceTestCase extends AbstractMuleTestCase
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException expectedException = none();

    @Test
    public void nonExistentBundle()
    {
        ComponentConfiguration componentConfiguration = new ComponentConfiguration.Builder()
                .setNamespace("mule")
                .setIdentifier("configuration")
                .build();

        expectedException.expect(BundleNotFoundException.class);
        getConnectionTestingService(componentConfiguration);
    }

    private ConnectivityTestingService getConnectionTestingService(ComponentConfiguration... componentConfigurations)
    {
        ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(componentConfigurations));
        MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry();
        ToolingService toolingService = new DefaultToolingService(createFakeRepositorySystem(), new TemporaryToolingArtifactBuilderFactory(muleArtifactResourcesRegistry));

        return toolingService.newConnectivityTestingServiceBuilder()
                .setArtifactConfiguration(artifactConfiguration)
                .addExtension("org.mule.extensions", "mule-module-file", "4.0-SNAPSHOT")
                .build();
    }

    private RepositoryService createFakeRepositorySystem()
    {
        RepositoryService mockRepositoryService = mock(RepositoryService.class);
        when(mockRepositoryService.lookupBundle(any())).thenThrow(BundleNotFoundException.class);
        return mockRepositoryService;
    }

}

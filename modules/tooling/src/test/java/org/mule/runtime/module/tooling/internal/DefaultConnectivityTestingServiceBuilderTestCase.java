/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.tooling.internal.DefaultConnectivityTestingServiceBuilder.NO_CONNECTIVITY_TESTING_STRATEGY_FOUND;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifactBuilderFactory;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DefaultConnectivityTestingServiceBuilderTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = none();

    private RepositoryService mockRepositoryService = mock(RepositoryService.class, RETURNS_DEEP_STUBS);
    private ToolingArtifactBuilderFactory mockArtifactBuilderFactory = mock(ToolingArtifactBuilderFactory.class, RETURNS_DEEP_STUBS);
    private ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class);
    private DefaultConnectivityTestingServiceBuilder builder;

    @Before
    public void createConnectivityTestingBuilder()
    {
        builder = new DefaultConnectivityTestingServiceBuilder(mockRepositoryService, mockArtifactBuilderFactory, mockServiceRegistry);
    }

    @Test(expected = IllegalStateException.class)
    public void noExtensionConfigured()
    {
        builder
                .setArtifactConfiguration(getArtifactConfiguration())
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void noArtifactConfiguration()
    {
        addExtension().build();
    }

    @Test
    public void buildArtifactNoConnectivityStrategies()
    {
        addExtension().setArtifactConfiguration(getArtifactConfiguration());

        expectedException.expectMessage(is(NO_CONNECTIVITY_TESTING_STRATEGY_FOUND));
        builder.build();
    }

    @Test
    public void buildArtifact()
    {
        when(mockServiceRegistry.lookupProviders(ConnectivityTestingStrategy.class)).thenReturn(asList(mock(ConnectivityTestingStrategy.class)));
        addExtension().setArtifactConfiguration(getArtifactConfiguration());

        builder.build();
    }
    private DefaultConnectivityTestingServiceBuilder addExtension()
    {
        builder.addExtension("groupId", "artifactId", "version");
        return builder;
    }

    private ArtifactConfiguration getArtifactConfiguration()
    {
        return new ArtifactConfiguration(asList(new ComponentConfiguration
                .Builder()
                                                        .setIdentifier("identifier")
                                                        .setNamespace("namespace").build()));
    }

}
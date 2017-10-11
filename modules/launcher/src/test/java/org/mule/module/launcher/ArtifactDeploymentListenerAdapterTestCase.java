/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mule.config.bootstrap.ArtifactType.ALL;
import static org.mule.config.bootstrap.ArtifactType.APP;
import static org.mule.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.module.launcher.ArtifactDeploymentListenerAdapter.AdaptedDeploymentListener.UNSUPPORTED_ARTIFACT_TYPE_ERROR;

import org.mule.api.MuleContext;
import org.mule.config.bootstrap.ArtifactType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ArtifactDeploymentListenerAdapterTestCase extends AbstractMuleTestCase
{

    private static final String ARTIFACT_NAME = "artifactName";

    @Rule
    public  ExpectedException expectedException = ExpectedException.none();

    @Mock
    private ArtifactDeploymentListener listener;

    @Mock
    private MuleContext context;

    private ArtifactDeploymentListenerAdapter adapter;

    @Before
    public void setUp() throws Exception
    {
        adapter = new ArtifactDeploymentListenerAdapter(listener);
    }

    @Test
    public void adapt() throws Exception
    {
        DeploymentListener applicationDeploymentListener = adapter.getApplicationDeploymentListener();
        DeploymentListener domainDeploymentListener = adapter.getDomainDeploymentListener();
        assertDeploymentListenerInvocations(applicationDeploymentListener, APP);
        assertDeploymentListenerInvocations(domainDeploymentListener, DOMAIN);
    }

    @Test
    public void invalidArtifactType()
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage(UNSUPPORTED_ARTIFACT_TYPE_ERROR);
        ArtifactDeploymentListenerAdapter.AdaptedDeploymentListener  adaptedDeploymentListener = new ArtifactDeploymentListenerAdapter.AdaptedDeploymentListener(listener, ALL);
    }

    private void assertDeploymentListenerInvocations(DeploymentListener deploymentListener, ArtifactType artifactType)
    {
        notifyDeploymentEvents(deploymentListener);
        verifyArtifactDeploymentListenerExecutions(artifactType);
        reset(listener);
    }

    private void notifyDeploymentEvents(DeploymentListener deploymentListener)
    {
        deploymentListener.onDeploymentStart(ARTIFACT_NAME);
        deploymentListener.onDeploymentFailure(ARTIFACT_NAME, null);
        deploymentListener.onDeploymentSuccess(ARTIFACT_NAME);
        deploymentListener.onUndeploymentStart(ARTIFACT_NAME);
        deploymentListener.onUndeploymentFailure(ARTIFACT_NAME, null);
        deploymentListener.onUndeploymentSuccess(ARTIFACT_NAME);
        deploymentListener.onMuleContextCreated(ARTIFACT_NAME, context);
        deploymentListener.onMuleContextConfigured(ARTIFACT_NAME, context);
        deploymentListener.onMuleContextInitialised(ARTIFACT_NAME, context);
    }

    private void verifyArtifactDeploymentListenerExecutions (ArtifactType artifactType)
    {
        verify(listener).onDeploymentStart(artifactType, ARTIFACT_NAME);
        verify(listener).onDeploymentFailure(artifactType, ARTIFACT_NAME, null);
        verify(listener).onDeploymentSuccess(artifactType, ARTIFACT_NAME);
        verify(listener).onUndeploymentStart(artifactType, ARTIFACT_NAME);
        verify(listener).onUndeploymentFailure(artifactType, ARTIFACT_NAME, null);
        verify(listener).onUndeploymentSuccess(artifactType, ARTIFACT_NAME);
        verify(listener).onMuleContextCreated(ARTIFACT_NAME, context);
        verify(listener).onMuleContextConfigured(ARTIFACT_NAME, context);
        verify(listener).onMuleContextInitialised(ARTIFACT_NAME, context);
    }

}
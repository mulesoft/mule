/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.MuleCoreExtension;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

@SmallTest
public class MuleContainerTestCase extends AbstractMuleTestCase
{

    // Required to run the test, otherwise we need to configure a fake mule
    // folder with a conf/log4j.properties
    @Rule
    public SystemProperty simpleLog = new SystemProperty("mule.simpleLog", "true");

    private MuleContainer container;

    private MuleCoreExtensionDiscoverer extensionDiscoverer;

    private DeploymentService deploymentService;

    @Before
    public void setUp() throws Exception
    {
        extensionDiscoverer = mock(MuleCoreExtensionDiscoverer.class);
        deploymentService = mock(DeploymentService.class);

        container = new MuleContainer(extensionDiscoverer, deploymentService);
    }

    @Test
    public void discoversCoreExtensions() throws Exception
    {
        container.start(false);

        Mockito.verify(extensionDiscoverer).discover();
    }

    @Test
    public void initializesDeploymentServiceAwareCoreExtension() throws Exception
    {
        Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> extensions = new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>();
        TestDeploymentServiceAwareExtension extension = mock(TestDeploymentServiceAwareExtension.class);
        extensions.put(TestDeploymentServiceAwareExtension.class, extension);
        when(extensionDiscoverer.discover()).thenReturn(extensions);

        container.start(false);

        verify(extension).setDeploymentService(deploymentService);
    }

    @Test
    public void initializesMuleCoreExtensionAwareCoreExtension() throws Exception
    {
        Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> extensions = new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>();
        TestMuleCoreExtensionAwareExtension extension = mock(TestMuleCoreExtensionAwareExtension.class);
        extensions.put(TestMuleCoreExtensionAwareExtension.class, extension);
        when(extensionDiscoverer.discover()).thenReturn(extensions);

        container.start(false);

        verify(extension).setMuleCoreExtensions(extensions);
    }

    @Test
    public void initializesDeploymentListenerCoreExtension() throws Exception
    {
        Map<Class<? extends MuleCoreExtension>, MuleCoreExtension> extensions = new HashMap<Class<? extends MuleCoreExtension>, MuleCoreExtension>();
        TestDeploymentListenerExtension extension = mock(TestDeploymentListenerExtension.class);
        extensions.put(TestDeploymentListenerExtension.class, extension);
        when(extensionDiscoverer.discover()).thenReturn(extensions);

        container.start(false);

        verify(deploymentService).addDeploymentListener(extension);
    }

    public static interface TestDeploymentServiceAwareExtension extends MuleCoreExtension, DeploymentServiceAware
    {

    }

    public static interface TestMuleCoreExtensionAwareExtension extends MuleCoreExtension, MuleCoreExtensionAware
    {

    }

    public static interface TestDeploymentListenerExtension extends MuleCoreExtension, DeploymentListener
    {

    }
}

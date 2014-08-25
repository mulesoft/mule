/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.coreextension.MuleCoreExtensionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@SmallTest
public class MuleContainerTestCase extends AbstractMuleTestCase
{

    // Required to run the test, otherwise we need to configure a fake mule
    // folder with a conf/log4j2.xml
    @Rule
    public SystemProperty simpleLog = new SystemProperty(MuleProperties.MULE_SIMPLE_LOG, "true");

    private MuleContainer container;

    private MuleCoreExtensionManager coreExtensionManager;

    private DeploymentService deploymentService;

    @Before
    public void setUp() throws Exception
    {
        coreExtensionManager = mock(MuleCoreExtensionManager.class);
        deploymentService = mock(DeploymentService.class);

        container = new MuleContainer(deploymentService, coreExtensionManager);
    }

    @Test
    public void startsMulecoreExtensionManager() throws Exception
    {
        container.start(false);

        Mockito.verify(coreExtensionManager).setDeploymentService(deploymentService);
        Mockito.verify(coreExtensionManager).initialise();
        Mockito.verify(coreExtensionManager).start();
    }

    @Test
    public void initializeCoreExtensionsBeforeStartingDeploymentService() throws Exception
    {
        container.start(false);

        final InOrder ordered = inOrder(coreExtensionManager, deploymentService);
        ordered.verify(coreExtensionManager).initialise();
        ordered.verify(deploymentService).start();
    }


    @Test
    public void startsCoreExtensionsBeforeDeploymentService() throws Exception
    {
        container.start(false);

        InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
        inOrder.verify(coreExtensionManager).start();
        inOrder.verify(deploymentService).start();
    }

    @Test
    public void stopsCoreExtensionsBeforeDeploymentService() throws Exception
    {
        container.start(false);
        container.stop();

        InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
        inOrder.verify(coreExtensionManager).stop();
        inOrder.verify(deploymentService).stop();
    }

    @Test
    public void disposesCoreExtensionsAfterStoppingDeploymentService() throws Exception
    {
        container.start(false);
        container.stop();

        InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
        inOrder.verify(deploymentService).stop();
        inOrder.verify(coreExtensionManager).dispose();
    }
}

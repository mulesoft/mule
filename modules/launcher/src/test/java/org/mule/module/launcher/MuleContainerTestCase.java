/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.module.launcher.MuleFoldersUtil.getExecutionFolder;

import org.mule.api.config.MuleProperties;
import org.mule.module.launcher.coreextension.MuleCoreExtensionManager;
import org.mule.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;

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
        FileUtils.deleteDirectory(getExecutionFolder());
    }

    @Test
    public void startsMulecoreExtensionManager() throws Exception
    {
        container.start(false);

        verify(coreExtensionManager).setDeploymentService(deploymentService);
        verify(coreExtensionManager).initialise();
        verify(coreExtensionManager).start();
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

    @Test
    public void disposesLogContextFactory() throws Exception
    {
        final LoggerContextFactory originalFactory = LogManager.getFactory();
        try
        {
            MuleLog4jContextFactory contextFactory = mock(MuleLog4jContextFactory.class);
            LogManager.setFactory(contextFactory);
            container.stop();

            verify(contextFactory).dispose();
        }
        finally
        {
            LogManager.setFactory(originalFactory);
        }
    }

    @Test
    public void onStartCreateExecutionFolderIfDoesNotExists() throws Exception
    {
        container.start(false);
        assertThat(getExecutionFolder().exists(), is(true));
    }

    @Test
    public void onStartAndExecutionFolderExistsDoNotFail() throws Exception
    {
        assertThat(getExecutionFolder().mkdirs(), is(true));
        container.start(false);
    }
}

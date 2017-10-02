/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.container.api.MuleFoldersUtil.getExecutionFolder;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.launcher.coreextension.MuleCoreExtensionManagerServer;
import org.mule.runtime.module.launcher.log4j2.MuleLog4jContextFactory;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.tooling.api.ToolingService;
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
public class MuleContainerTestCase extends AbstractMuleTestCase {

  // Required to run the test, otherwise we need to configure a fake mule
  // folder with a conf/log4j2.xml
  @Rule
  public SystemProperty simpleLog = new SystemProperty(MuleProperties.MULE_SIMPLE_LOG, "true");

  private MuleContainer container;

  private MuleCoreExtensionManagerServer coreExtensionManager;

  private DeploymentService deploymentService = mock(DeploymentService.class);

  private RepositoryService repositoryService = mock(RepositoryService.class);

  private final ServiceManager serviceManager = mock(ServiceManager.class);

  private final ExtensionModelLoaderManager extensionModelLoaderManager = mock(ExtensionModelLoaderManager.class);

  private ToolingService toolingService = mock(ToolingService.class);

  @Before
  public void setUp() throws Exception {
    coreExtensionManager = mock(MuleCoreExtensionManagerServer.class);
    container = new MuleContainer(deploymentService, repositoryService, toolingService, coreExtensionManager, serviceManager,
                                  extensionModelLoaderManager);
    FileUtils.deleteDirectory(getExecutionFolder());
  }

  @Test
  public void startsMuleCoreExtensionManager() throws Exception {
    container.start(false);

    verify(coreExtensionManager).setDeploymentService(deploymentService);
    verify(coreExtensionManager).setRepositoryService(repositoryService);
    verify(coreExtensionManager).setToolingService(toolingService);
    verify(coreExtensionManager).initialise();
    verify(coreExtensionManager).start();
  }

  @Test
  public void initializeCoreExtensionsBeforeStartingDeploymentService() throws Exception {
    container.start(false);

    final InOrder ordered = inOrder(coreExtensionManager, deploymentService);
    ordered.verify(coreExtensionManager).initialise();
    ordered.verify(deploymentService).start();
  }


  @Test
  public void startsCoreExtensionsBeforeDeploymentService() throws Exception {
    container.start(false);

    InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
    inOrder.verify(coreExtensionManager).start();
    inOrder.verify(deploymentService).start();
  }

  @Test
  public void stopsCoreExtensionsBeforeDeploymentService() throws Exception {
    container.start(false);
    container.stop();

    InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
    inOrder.verify(coreExtensionManager).stop();
    inOrder.verify(deploymentService).stop();
  }

  @Test
  public void disposesCoreExtensionsAfterStoppingDeploymentService() throws Exception {
    container.start(false);
    container.stop();

    InOrder inOrder = inOrder(coreExtensionManager, deploymentService);
    inOrder.verify(deploymentService).stop();
    inOrder.verify(coreExtensionManager).dispose();
  }

  @Test
  public void disposesLogContextFactory() throws Exception {
    final LoggerContextFactory originalFactory = LogManager.getFactory();
    try {
      MuleLog4jContextFactory contextFactory = mock(MuleLog4jContextFactory.class);
      LogManager.setFactory(contextFactory);
      container.stop();

      verify(contextFactory).dispose();
    } finally {
      LogManager.setFactory(originalFactory);
    }
  }

  @Test
  public void onStartCreateExecutionFolderIfDoesNotExists() throws Exception {
    container.start(false);
    assertThat(getExecutionFolder().exists(), is(true));
  }

  @Test
  public void onStartAndExecutionFolderExistsDoNotFail() throws Exception {
    assertThat(getExecutionFolder().mkdirs(), is(true));
    container.start(false);
  }

  @Test
  public void startsServiceManagerBeforeDeploymentService() throws Exception {
    container.start(false);

    InOrder inOrder = inOrder(serviceManager, deploymentService);
    inOrder.verify(serviceManager).start();
    inOrder.verify(deploymentService).start();
  }

  @Test
  public void stopsServiceManagerAfterDeploymentService() throws Exception {
    container.start(false);
    container.stop();

    InOrder inOrder = inOrder(serviceManager, deploymentService);
    inOrder.verify(deploymentService).stop();
    inOrder.verify(serviceManager).stop();
  }
}

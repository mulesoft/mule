/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.container.api.CoreExtensionsAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.DeploymentService;
import org.mule.runtime.module.launcher.DeploymentServiceAware;
import org.mule.runtime.module.launcher.RepositoryServiceAware;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;
import org.mockito.InOrder;

@SmallTest
public class DefaultMuleCoreExtensionManagerTestCase extends AbstractMuleTestCase {

  private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer = mock(MuleCoreExtensionDiscoverer.class);
  private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver =
      mock(MuleCoreExtensionDependencyResolver.class);
  private MuleCoreExtensionManagerServer coreExtensionManager =
      new DefaultMuleCoreExtensionManagerServer(coreExtensionDiscoverer, coreExtensionDependencyResolver);

  @Test
  public void discoversMuleCoreExtension() throws Exception {
    coreExtensionManager.initialise();

    verify(coreExtensionDiscoverer).discover();
  }

  @Test
  public void injectsDeploymentServiceAwareCoreExtension() throws Exception {
    Consumer<DeploymentService> setServiceFunction = (service) -> coreExtensionManager.setDeploymentService(service);
    BiConsumer<List<TestDeploymentServiceAwareExtension>, DeploymentService> verificationFunction =
        (extensions, service) -> verify(extensions.get(0)).setDeploymentService(service);
    testServiceInjection(DeploymentService.class, TestDeploymentServiceAwareExtension.class, setServiceFunction,
                         verificationFunction);
  }

  @Test
  public void injectRepositoryServiceAwareCoreExtension() throws Exception {
    Consumer<RepositoryService> setServiceFunction = (service) -> coreExtensionManager.setRepositoryService(service);
    BiConsumer<List<TestRepositoryServiceAwareExtension>, RepositoryService> verificationFunction =
        (extensions, service) -> verify(extensions.get(0)).setRepositoryService(service);
    testServiceInjection(RepositoryService.class, TestRepositoryServiceAwareExtension.class, setServiceFunction,
                         verificationFunction);
  }

  @Test
  public void initializesDeploymentListenerCoreExtension() throws Exception {

    Consumer<DeploymentService> setServiceFunction = (service) -> coreExtensionManager.setDeploymentService(service);
    BiConsumer<List<TestDeploymentListenerExtension>, DeploymentService> verificationFunction =
        (extensions, service) -> verify(service).addDeploymentListener(extensions.get(0));
    testServiceInjection(DeploymentService.class, TestDeploymentListenerExtension.class, setServiceFunction,
                         verificationFunction);
  }

  @Test
  public void injectsCoreExtensionsAwareCoreExtension() throws Exception {
    Consumer<DeploymentService> setServiceFunction = (service) -> {
    };
    BiConsumer<List<TestCoreExtensionsAwareExtension>, DeploymentService> verificationFunction =
        (extensions, service) -> verify(extensions.get(0)).setCoreExtensions(new ArrayList<>(extensions));
    testServiceInjection(DeploymentService.class, TestCoreExtensionsAwareExtension.class, setServiceFunction,
                         verificationFunction);
  }

  @Test
  public void startsCoreExtensionsInOrder() throws Exception {
    List<MuleCoreExtension> extensions = new LinkedList<>();
    MuleCoreExtension extension1 = mock(MuleCoreExtension.class);
    MuleCoreExtension extension2 = mock(MuleCoreExtension.class);
    extensions.add(extension1);
    extensions.add(extension2);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

    List<MuleCoreExtension> orderedExtensions = new LinkedList<>();
    orderedExtensions.add(extension2);
    orderedExtensions.add(extension1);
    when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(orderedExtensions);
    coreExtensionManager.initialise();

    coreExtensionManager.start();

    InOrder ordered = inOrder(extension1, extension2);
    ordered.verify(extension2).start();
    ordered.verify(extension1).start();
  }


  @Test
  public void stopsCoreExtensionsInOrder() throws Exception {
    List<MuleCoreExtension> extensions = new LinkedList<>();
    MuleCoreExtension extension1 = mock(MuleCoreExtension.class);
    MuleCoreExtension extension2 = mock(MuleCoreExtension.class);
    extensions.add(extension1);
    extensions.add(extension2);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

    List<MuleCoreExtension> orderedExtensions = new LinkedList<>();
    orderedExtensions.add(extension1);
    orderedExtensions.add(extension2);
    when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(orderedExtensions);
    coreExtensionManager.initialise();

    coreExtensionManager.stop();

    InOrder ordered = inOrder(extension1, extension2);
    ordered.verify(extension2).stop();
    ordered.verify(extension1).stop();
  }

  @Test
  public void initializesCoreExtensionsInOrder() throws Exception {
    List<MuleCoreExtension> extensions = new LinkedList<>();
    MuleCoreExtension extension1 = mock(MuleCoreExtension.class);
    MuleCoreExtension extension2 = mock(MuleCoreExtension.class);
    extensions.add(extension1);
    extensions.add(extension2);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

    List<MuleCoreExtension> orderedExtensions = new LinkedList<>();
    orderedExtensions.add(extension2);
    orderedExtensions.add(extension1);
    when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(orderedExtensions);
    coreExtensionManager.initialise();

    InOrder ordered = inOrder(extension1, extension2);
    ordered.verify(extension2).initialise();
    ordered.verify(extension1).initialise();
  }

  @Test
  public void disposesCoreExtensions() throws Exception {
    List<MuleCoreExtension> extensions = new LinkedList<>();
    TestDeploymentServiceAwareExtension extension1 = mock(TestDeploymentServiceAwareExtension.class);
    MuleCoreExtension extension2 = mock(MuleCoreExtension.class);
    extensions.add(extension1);
    extensions.add(extension2);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

    List<MuleCoreExtension> orderedExtensions = new LinkedList<>();
    orderedExtensions.add(extension1);
    orderedExtensions.add(extension2);
    when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(orderedExtensions);
    coreExtensionManager.initialise();

    coreExtensionManager.dispose();

    InOrder inOrder = inOrder(extension1, extension2);
    inOrder.verify(extension1).dispose();
    inOrder.verify(extension2).dispose();
  }

  @Test
  public void resolvesCoreExtensionDependencies() throws Exception {

    List<MuleCoreExtension> extensions = new LinkedList<>();
    MuleCoreExtension extension = mock(MuleCoreExtension.class);
    extensions.add(extension);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

    coreExtensionManager.initialise();

    verify(coreExtensionDependencyResolver).resolveDependencies(extensions);
  }

  private <ServiceType, CoreExtensionType extends MuleCoreExtension> void testServiceInjection(Class<ServiceType> serviceType,
                                                                                               Class<CoreExtensionType> coreExtensionType,
                                                                                               Consumer<ServiceType> setServiceFunction,
                                                                                               BiConsumer<List<CoreExtensionType>, ServiceType> verificationFunction)
      throws DefaultMuleException, InitialisationException {
    List<MuleCoreExtension> extensions = new LinkedList<>();
    CoreExtensionType extension = mock(coreExtensionType);
    extensions.add(extension);
    ServiceType service = mock(serviceType);
    when(coreExtensionDiscoverer.discover()).thenReturn(extensions);
    when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(extensions);
    setServiceFunction.accept(service);
    coreExtensionManager.initialise();
    verificationFunction.accept((List<CoreExtensionType>) extensions, service);
  }

  public interface TestDeploymentServiceAwareExtension extends MuleCoreExtension, DeploymentServiceAware {

  }

  public interface TestRepositoryServiceAwareExtension extends MuleCoreExtension, RepositoryServiceAware {

  }

  public interface TestDeploymentListenerExtension extends MuleCoreExtension, DeploymentListener {

  }

  public interface TestCoreExtensionsAwareExtension extends MuleCoreExtension, CoreExtensionsAware {

  }

}

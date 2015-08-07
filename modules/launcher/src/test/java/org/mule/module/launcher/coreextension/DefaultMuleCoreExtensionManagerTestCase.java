/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.CoreExtensionsAware;
import org.mule.MuleCoreExtension;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentService;
import org.mule.module.launcher.DeploymentServiceAware;
import org.mule.module.launcher.PluginClassLoaderManager;
import org.mule.module.launcher.PluginClassLoaderManagerAware;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

@SmallTest
public class DefaultMuleCoreExtensionManagerTestCase extends AbstractMuleTestCase
{

    private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer = mock(MuleCoreExtensionDiscoverer.class);
    private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver = mock(MuleCoreExtensionDependencyResolver.class);
    private MuleCoreExtensionManager coreExtensionManager = new DefaultMuleCoreExtensionManager(coreExtensionDiscoverer, coreExtensionDependencyResolver);

    @Test
    public void discoversMuleCoreExtension() throws Exception
    {
        coreExtensionManager.initialise();

        verify(coreExtensionDiscoverer).discover();
    }

    @Test
    public void injectsDeploymentServiceAwareCoreExtension() throws Exception
    {
        List<MuleCoreExtension> extensions = new LinkedList<>();
        TestDeploymentServiceAwareExtension extension = mock(TestDeploymentServiceAwareExtension.class);
        extensions.add(extension);
        when(coreExtensionDiscoverer.discover()).thenReturn(extensions);
        when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(extensions);

        DeploymentService deploymentService = mock(DeploymentService.class);
        coreExtensionManager.setDeploymentService(deploymentService);

        coreExtensionManager.initialise();

        verify(extension).setDeploymentService(deploymentService);
    }

    @Test
    public void initializesDeploymentListenerCoreExtension() throws Exception
    {
        List<MuleCoreExtension> extensions = new LinkedList<>();
        TestDeploymentListenerExtension extension = mock(TestDeploymentListenerExtension.class);
        extensions.add(extension);
        when(coreExtensionDiscoverer.discover()).thenReturn(extensions);
        when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(extensions);

        DeploymentService deploymentService = mock(DeploymentService.class);
        coreExtensionManager.setDeploymentService(deploymentService);

        coreExtensionManager.initialise();

        verify(deploymentService).addDeploymentListener(extension);
    }

    @Test
    public void injectsPluginClassLoaderAwareCoreExtension() throws Exception
    {
        List<MuleCoreExtension> extensions = new LinkedList<>();
        TestPluginClassLoaderManagerAwareExtension extension = mock(TestPluginClassLoaderManagerAwareExtension.class);
        extensions.add(extension);
        when(coreExtensionDiscoverer.discover()).thenReturn(extensions);
        when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(extensions);

        PluginClassLoaderManager pluginClassLoaderManager = mock(PluginClassLoaderManager.class);
        coreExtensionManager.setPluginClassLoaderManager(pluginClassLoaderManager);

        coreExtensionManager.initialise();

        verify(extension).setPluginClassLoaderManager(pluginClassLoaderManager);
    }

    @Test
    public void injectsCoreExtensionsAwareCoreExtension() throws Exception
    {
        List<MuleCoreExtension> extensions = new LinkedList<>();
        TestCoreExtensionsAwareExtension extension = mock(TestCoreExtensionsAwareExtension.class);
        extensions.add(extension);
        when(coreExtensionDiscoverer.discover()).thenReturn(extensions);
        when(coreExtensionDependencyResolver.resolveDependencies(extensions)).thenReturn(extensions);

        coreExtensionManager.initialise();

        verify(extension).setCoreExtensions(extensions);
    }

    @Test
    public void startsCoreExtensionsInOrder() throws Exception
    {
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
    public void stopsCoreExtensionsInOrder() throws Exception
    {
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
    public void initializesCoreExtensionsInOrder() throws Exception
    {
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
    public void disposesCoreExtensions() throws Exception
    {
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
    public void resolvesCoreExtensionDependencies() throws Exception
    {

        List<MuleCoreExtension> extensions = new LinkedList<>();
        MuleCoreExtension extension = mock(MuleCoreExtension.class);
        extensions.add(extension);
        when(coreExtensionDiscoverer.discover()).thenReturn(extensions);

        coreExtensionManager.initialise();

        verify(coreExtensionDependencyResolver).resolveDependencies(extensions);
    }

    public interface TestDeploymentServiceAwareExtension extends MuleCoreExtension, DeploymentServiceAware
    {

    }

    public interface TestDeploymentListenerExtension extends MuleCoreExtension, DeploymentListener
    {

    }

    public interface TestPluginClassLoaderManagerAwareExtension extends MuleCoreExtension, PluginClassLoaderManagerAware
    {

    }

    public interface TestCoreExtensionsAwareExtension extends MuleCoreExtension, CoreExtensionsAware
    {

    }

}

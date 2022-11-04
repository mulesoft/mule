/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.test.allure.AllureConstants.DeployableCreationFeature.APP_CREATION;

import static java.util.Optional.empty;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilder;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.artifact.extension.ExtensionModelLoaderManager;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainManager;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(APP_CREATION)
@Issue("W-11086334")
public class DefaultLightweightApplicationFactoryTestCase extends AbstractMuleTestCase {

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);

  private static final String SOCKET_CONNECTOR = "/plugin/Sockets";
  private static final String HTTP_CONNECTOR = "/plugin/HTTP";
  private MuleApplicationClassLoader classloader;

  private final DomainManager domainManager = new DefaultDomainManager();
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory,
                                    DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                                    domainManager,
                                    mock(ServiceRepository.class),
                                    mock(ExtensionModelLoaderManager.class),
                                    mock(ClassLoaderRepository.class),
                                    mock(PolicyTemplateClassLoaderBuilderFactory.class),
                                    mock(PluginDependenciesResolver.class),
                                    mock(LicenseValidator.class),
                                    getRuntimeLockFactory(),
                                    mock(MemoryManagementService.class),
                                    mock(ArtifactConfigurationProcessor.class));

  public DefaultLightweightApplicationFactoryTestCase() {
    classloader = mock(MuleApplicationClassLoader.class);
    ApplicationClassLoaderBuilder applicationClassLoaderBuilderMock = mock(ApplicationClassLoaderBuilder.class);
    when(applicationClassLoaderBuilderMock.setDomainParentClassLoader(any())).thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.setArtifactDescriptor(any()))
        .thenReturn(applicationClassLoaderBuilderMock);
    when(applicationClassLoaderBuilderMock.build()).thenReturn(classloader);
    when(applicationClassLoaderBuilderFactory.createArtifactClassLoaderBuilder())
        .thenReturn(applicationClassLoaderBuilderMock);

    final ArtifactPluginDescriptor socketArtifactPluginDescriptor = new ArtifactPluginDescriptor(SOCKET_CONNECTOR);
    socketArtifactPluginDescriptor
        .setClassLoaderConfiguration(new ClassLoaderConfiguration.ClassLoaderConfigurationBuilder().build());
    final ArtifactPlugin socketPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader socketClassloader = mock(ArtifactClassLoader.class);
    when(socketPlugin.getArtifactClassLoader()).thenReturn(socketClassloader);
    when(socketClassloader.getArtifactId()).thenReturn(SOCKET_CONNECTOR);
    when(socketPlugin.getDescriptor()).thenReturn(socketArtifactPluginDescriptor);


    final ArtifactPluginDescriptor httpArtifactPluginDescriptor = new ArtifactPluginDescriptor(HTTP_CONNECTOR);
    socketArtifactPluginDescriptor
        .setClassLoaderConfiguration(new ClassLoaderConfiguration.ClassLoaderConfigurationBuilder().build());
    final ArtifactPlugin httpPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader httpClassloader = mock(ArtifactClassLoader.class);
    when(httpPlugin.getArtifactClassLoader()).thenReturn(httpClassloader);
    when(httpClassloader.getArtifactId()).thenReturn(HTTP_CONNECTOR);
    when(httpPlugin.getDescriptor()).thenReturn(httpArtifactPluginDescriptor);

    List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
    pluginClassLoaders.add(socketClassloader);
    pluginClassLoaders.add(httpClassloader);
    when(classloader.getArtifactPluginClassLoaders()).thenReturn(pluginClassLoaders);

    createDefaultDomain();
  }

  private void createDefaultDomain() {
    final Domain defaultDomain = mock(Domain.class);

    final ArtifactClassLoader domainArtifactClassLoader = mock(ArtifactClassLoader.class);
    when(domainArtifactClassLoader.getClassLoader()).thenReturn(mock(ClassLoader.class));
    when(defaultDomain.getArtifactClassLoader()).thenReturn(domainArtifactClassLoader);
    when(defaultDomain.getDescriptor()).thenReturn(new DomainDescriptor(DEFAULT_DOMAIN_NAME));

    domainManager.addDomain(defaultDomain);
  }

  @Test
  public void lightweightApplication() throws Exception {
    String appName = "no-dependencies";
    when(classloader.getArtifactId()).thenReturn(appName);

    Application application = applicationFactory.createArtifact(getApplicationFolder("apps/" + appName), empty());

    assertThat(application.getDescriptor(), instanceOf(ApplicationDescriptor.class));
    assertThat(application.getDescriptor().getName(), is(appName));
    assertThat(application.getDescriptor().getConfigResources(), contains("mule-config.xml"));
    assertThat(application.getArtifactPlugins().size(), is(0));
    assertThat(application.getDescriptor().getName(), is("no-dependencies"));
    assertThat(application.getDescriptor().getConfigResources(), contains("mule-config.xml"));
  }

  @Test
  public void lightweightApplicationWithDependency() throws Exception {
    String appName = "multiple-dependencies";
    when(classloader.getArtifactId()).thenReturn(appName);

    Application application = applicationFactory.doCreateArtifact(getApplicationFolder("apps/" + appName), empty());

    assertThat(application.getDescriptor(), instanceOf(ApplicationDescriptor.class));
    assertThat(application.getDescriptor().getName(), is(appName));
    assertThat(application.getDescriptor().getConfigResources(), contains("mule-config.xml"));
    List<ArtifactPlugin> plugins = application.getArtifactPlugins();
    assertThat(plugins.size(), is(2));
    assertThat(plugins, contains(
                                 hasProperty("artifactId", is(appName + "/plugin/Sockets")),
                                 hasProperty("artifactId", is(appName + "/plugin/HTTP"))));
  }

  protected File getApplicationFolder(String path) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(path).toURI());
  }

}

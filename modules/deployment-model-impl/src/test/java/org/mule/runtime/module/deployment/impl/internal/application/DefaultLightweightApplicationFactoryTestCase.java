/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.test.allure.AllureConstants.DeployableCreationFeature.APP_CREATION;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.LIGHTWEIGHT;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

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
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.internal.classloader.MuleApplicationClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainManager;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(APP_CREATION)
@Story(LIGHTWEIGHT)
@Issue("W-11086334")
public class DefaultLightweightApplicationFactoryTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty repositoryLocation = new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                                                                discoverProvider(ApplicationDescriptorFactoryTestCase.class
                                                                    .getClassLoader()).getLocalRepositorySuppliers()
                                                                        .environmentMavenRepositorySupplier().get()
                                                                        .getAbsolutePath());

  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
      mock(ApplicationClassLoaderBuilderFactory.class);

  private static final String EMPTY_PLUGIN = "/plugin/empty-plugin";
  private static final String DEPENDANT_CONNECTOR = "/plugin/dependant-plugin";
  private MuleApplicationClassLoader classloader;

  private final DomainManager domainManager = new DefaultDomainManager();
  private final DefaultApplicationFactory applicationFactory =
      new DefaultApplicationFactory(applicationClassLoaderBuilderFactory,
                                    DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                                    domainManager,
                                    mock(ServiceRepository.class),
                                    mock(ExtensionModelLoaderRepository.class),
                                    mock(ClassLoaderRepository.class),
                                    mock(PolicyTemplateClassLoaderBuilderFactory.class),
                                    mock(PluginDependenciesResolver.class),
                                    mock(LicenseValidator.class),
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

    final ArtifactPluginDescriptor emptyPluginArtifactPluginDescriptor = new ArtifactPluginDescriptor(EMPTY_PLUGIN);
    emptyPluginArtifactPluginDescriptor
        .setClassLoaderConfiguration(new ClassLoaderConfiguration.ClassLoaderConfigurationBuilder().build());
    final ArtifactPlugin emptyPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader emptyPluginClassloader = mock(ArtifactClassLoader.class);
    when(emptyPlugin.getArtifactClassLoader()).thenReturn(emptyPluginClassloader);
    when(emptyPluginClassloader.getArtifactId()).thenReturn(EMPTY_PLUGIN);
    when(emptyPlugin.getDescriptor()).thenReturn(emptyPluginArtifactPluginDescriptor);


    final ArtifactPluginDescriptor dependantArtifactPluginDescriptor = new ArtifactPluginDescriptor(DEPENDANT_CONNECTOR);
    dependantArtifactPluginDescriptor
        .setClassLoaderConfiguration(new ClassLoaderConfiguration.ClassLoaderConfigurationBuilder().build());
    final ArtifactPlugin dependantPlugin = mock(ArtifactPlugin.class);
    final ArtifactClassLoader dependantClassloader = mock(ArtifactClassLoader.class);
    when(dependantPlugin.getArtifactClassLoader()).thenReturn(dependantClassloader);
    when(dependantClassloader.getArtifactId()).thenReturn(DEPENDANT_CONNECTOR);
    when(dependantPlugin.getDescriptor()).thenReturn(dependantArtifactPluginDescriptor);

    List<ArtifactClassLoader> pluginClassLoaders = new ArrayList<>();
    pluginClassLoaders.add(emptyPluginClassloader);
    pluginClassLoaders.add(dependantClassloader);
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

  @Before
  public void before() {
    GlobalConfigLoader.reset();
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
                                 hasProperty("artifactId", is(appName + EMPTY_PLUGIN)),
                                 hasProperty("artifactId", is(appName + DEPENDANT_CONNECTOR))));
  }

  protected File getApplicationFolder(String path) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(path).toURI());
  }
}

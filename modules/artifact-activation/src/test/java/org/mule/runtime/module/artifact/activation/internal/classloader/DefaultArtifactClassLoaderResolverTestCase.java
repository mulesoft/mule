/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleLibFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver.getApplicationId;
import static org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver.getDomainId;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_GENERATION;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.qameta.allure.Issue;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_GENERATION)
public class DefaultArtifactClassLoaderResolverTestCase extends AbstractMuleTestCase {

  public static final String MULE_DOMAIN_FOLDER = "domains";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public TemporaryFolder artifactLocation = new TemporaryFolder();

  @Rule
  public final SystemProperty muleHomeSystemProperty =
      new SystemProperty(MULE_HOME_DIRECTORY_PROPERTY, temporaryFolder.getRoot().getCanonicalPath());

  private final File muleHomeFolder;

  private DefaultArtifactClassLoaderResolver artifactClassLoaderResolver;
  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();

  private static final String PRIVILEGED_PACKAGE = "org.foo.privileged";
  private static final String GROUP_ID = "org.mule.test";
  private static final String PLUGIN_ID1 = "plugin1";
  private static final String PLUGIN_ARTIFACT_ID1 = GROUP_ID + ":" + PLUGIN_ID1;
  private static final String PLUGIN_ID2 = "plugin2";
  private static final BundleDescriptor PLUGIN1_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID1)
          .setVersion("1.0").setClassifier("mule-plugin").build();
  private static final BundleDescriptor PLUGIN2_BUNDLE_DESCRIPTOR =
      new BundleDescriptor.Builder().setGroupId(GROUP_ID).setArtifactId(
                                                                        PLUGIN_ID2)
          .setVersion("1.0").setClassifier("mule-plugin").build();

  private final ArtifactPluginDescriptor plugin1Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID1);
  private final ArtifactPluginDescriptor plugin2Descriptor = new ArtifactPluginDescriptor(PLUGIN_ID2);

  public DefaultArtifactClassLoaderResolverTestCase() throws IOException {
    muleHomeFolder = temporaryFolder.getRoot();
  }

  @Before
  public void setup() {
    artifactClassLoaderResolver = spy(new DefaultArtifactClassLoaderResolver(moduleRepository, nativeLibraryFinderFactory));

    plugin1Descriptor.setBundleDescriptor(PLUGIN1_BUNDLE_DESCRIPTOR);
    plugin2Descriptor.setBundleDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR);
  }

  @After
  public void tearDown() {
    deleteIfNeeded(getDomainsFolder());
    deleteIfNeeded(new File(getMuleLibFolder(), "shared"));
  }

  private void deleteIfNeeded(File file) {
    if (file.exists()) {
      deleteQuietly(file);
    }
  }

  @Test
  public void createDomainDefaultClassLoader() {
    DomainDescriptor descriptor = getTestDomainDescriptor(DEFAULT_DOMAIN_NAME);
    final String artifactId = getDomainId(DEFAULT_DOMAIN_NAME);

    final ArtifactClassLoader domainClassLoader =
        artifactClassLoaderResolver.createDomainClassLoader(descriptor);

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test
  public void createDomainCustomClassLoader() {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDomainDescriptor(domainName);
    final String artifactId = getDomainId(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));

    final ArtifactClassLoader domainClassLoader =
        artifactClassLoaderResolver.createDomainClassLoader(descriptor);

    assertThat(domainClassLoader.getClassLoader(), instanceOf(MuleSharedDomainClassLoader.class));
    assertThat(domainClassLoader.getArtifactId(), equalTo(artifactId));
  }

  @Test(expected = ArtifactActivationException.class)
  public void validateDomainBeforeCreatingClassLoader() {
    DomainDescriptor descriptor = getTestDomainDescriptor("some-domain");
    descriptor.setRootFolder(new File("non-existent"));

    artifactClassLoaderResolver.createDomainClassLoader(descriptor);
  }

  @Test
  public void createDomainClassLoaderWithExportedPackages() {
    MuleModule muleModule = mock(MuleModule.class);
    final String repeatedPackageName = "module&domain-package";
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
    when(moduleRepository.getModules()).thenReturn(singletonList(muleModule));
    final String onlyDomainPackageName = "domain-package";

    final MuleDeployableArtifactClassLoader domainClassLoader =
        getTestDomainClassLoader(emptyList(), Stream.of(onlyDomainPackageName, repeatedPackageName).collect(toSet()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) domainClassLoader.getParent();

    assertThat(domainClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(repeatedPackageName),
               instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(regionClassLoader.filterForClassLoader(regionClassLoader.getOwnerClassLoader())
        .exportsPackage(repeatedPackageName), is(false));
    assertThat(regionClassLoader.filterForClassLoader(regionClassLoader.getOwnerClassLoader())
        .exportsPackage(onlyDomainPackageName),
               is(true));
  }

  @Test
  public void createDomainClassLoaderWithPlugins() {
    final MuleDeployableArtifactClassLoader domainClassLoader =
        getTestDomainClassLoader(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toList()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) domainClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  public void createDomainClassLoaderWithCachedPlugin() {
    MuleDeployableArtifactClassLoader domainClassLoader =
        getTestDomainClassLoader(emptyList());

    final DomainDescriptor newDomainDescriptor = domainClassLoader.getArtifactDescriptor();
    newDomainDescriptor.setPlugins(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet()));

    final MuleArtifactClassLoader plugin2ClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(domainClassLoader, plugin2Descriptor,
                                     (apds, d) -> empty());

    domainClassLoader =
        artifactClassLoaderResolver.createDomainClassLoader(newDomainDescriptor,
                                                            (ownerClassLoader, pluginDescriptor) -> {
                                                              if (pluginDescriptor
                                                                  .getBundleDescriptor()
                                                                  .getArtifactId()
                                                                  .equals(plugin2Descriptor
                                                                      .getBundleDescriptor()
                                                                      .getArtifactId())) {
                                                                return of(() -> plugin2ClassLoader);
                                                              } else {
                                                                return empty();
                                                              }
                                                            });

    verify(artifactClassLoaderResolver, times(1)).createMulePluginClassLoader(any(), eq(plugin2Descriptor), any());

    final RegionClassLoader regionClassLoader = (RegionClassLoader) domainClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  @Issue("W-11210306")
  public void createDomainClassLoaderWithPluginsSharingExportedPackages() {
    String exportedPackage = "plugin-package";

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(exportedPackage))
            .dependingOn(singleton(pluginDependency)).build());
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(exportedPackage))
            .build());

    final MuleDeployableArtifactClassLoader domainClassLoader =
        getTestDomainClassLoader(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toList()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) domainClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  public void createApplicationClassLoader() {
    final String applicationName = "app";

    final MuleDeployableArtifactClassLoader domainClassLoader = getTestDomainClassLoader(emptyList());
    final String artifactId = getApplicationId(domainClassLoader.getArtifactId(), applicationName);

    ApplicationDescriptor descriptor = new ApplicationDescriptor(applicationName);
    descriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    final ArtifactClassLoader artifactClassLoader = artifactClassLoaderResolver
        .createApplicationClassLoader(descriptor, () -> domainClassLoader);

    assertThat(artifactClassLoader.getClassLoader(), instanceOf(MuleApplicationClassLoader.class));

    final MuleApplicationClassLoader applicationClassLoader = (MuleApplicationClassLoader) artifactClassLoader;
    assertThat(applicationClassLoader.getParent().getParent(), is(domainClassLoader.getClassLoader()));
    assertThat(applicationClassLoader.getArtifactId(), is(artifactId));
  }

  @Test
  public void createApplicationClassLoaderWithExportedPackages() {
    MuleModule muleModule = mock(MuleModule.class);
    final String repeatedPackageName = "module&app-package";
    when(muleModule.getExportedPackages()).thenReturn(singleton(repeatedPackageName));
    when(moduleRepository.getModules()).thenReturn(singletonList(muleModule));
    final String onlyAppPackageName = "app-package";

    final MuleDeployableArtifactClassLoader applicationClassLoader =
        getTestApplicationClassLoader(emptyList(), Stream.of(onlyAppPackageName, repeatedPackageName).collect(toSet()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(applicationClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(repeatedPackageName),
               instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(regionClassLoader.filterForClassLoader(regionClassLoader.getOwnerClassLoader())
        .exportsPackage(repeatedPackageName), is(false));
    assertThat(regionClassLoader.filterForClassLoader(regionClassLoader.getOwnerClassLoader()).exportsPackage(onlyAppPackageName),
               is(true));
  }

  @Test
  public void createApplicationClassLoaderWithPlugins() {
    final MuleDeployableArtifactClassLoader applicationClassLoader =
        getTestApplicationClassLoader(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toList()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  public void createApplicationClassLoaderWithCachedPlugin() {
    MuleDeployableArtifactClassLoader applicationClassLoader =
        getTestApplicationClassLoader(emptyList());

    final ApplicationDescriptor newApplicationDescriptor = applicationClassLoader.getArtifactDescriptor();
    newApplicationDescriptor.setPlugins(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toSet()));

    final MuleArtifactClassLoader domainClassLoader = (MuleArtifactClassLoader) applicationClassLoader.getParent().getParent();

    final MuleArtifactClassLoader plugin2ClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin2Descriptor,
                                     (apds, d) -> empty());

    applicationClassLoader =
        artifactClassLoaderResolver.createApplicationClassLoader(newApplicationDescriptor,
                                                                 () -> domainClassLoader,
                                                                 (ownerClassLoader, pluginDescriptor) -> {
                                                                   if (pluginDescriptor
                                                                       .getBundleDescriptor()
                                                                       .getArtifactId()
                                                                       .equals(plugin2Descriptor
                                                                           .getBundleDescriptor()
                                                                           .getArtifactId())) {
                                                                     return of(() -> plugin2ClassLoader);
                                                                   } else {
                                                                     return empty();
                                                                   }
                                                                 });

    verify(artifactClassLoaderResolver, times(1)).createMulePluginClassLoader(any(), eq(plugin2Descriptor), any());

    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  @Issue("W-11210306")
  public void createApplicationClassLoaderWithPluginsSharingExportedPackages() {
    String exportedPackage = "plugin-package";

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(exportedPackage))
            .dependingOn(singleton(pluginDependency)).build());
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(exportedPackage))
            .build());

    final MuleDeployableArtifactClassLoader applicationClassLoader =
        getTestApplicationClassLoader(Stream.of(plugin1Descriptor, plugin2Descriptor).collect(toList()));
    final RegionClassLoader regionClassLoader = (RegionClassLoader) applicationClassLoader.getParent();

    assertThat(regionClassLoader.getArtifactPluginClassLoaders().stream().map(ArtifactClassLoader::getArtifactDescriptor)
        .collect(toList()), containsInAnyOrder(plugin1Descriptor, plugin2Descriptor));
  }

  @Test
  public void createDependantPluginClassLoader() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());
    String plugin2ExportedPackage = "plugin2-package";
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());
    plugin2Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(plugin2ExportedPackage))
            .build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> of(plugin2Descriptor));

    assertThat(pluginClassLoader.getParent(), is(applicationClassLoader.getParent()));
    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(plugin2ExportedPackage), is(PARENT_FIRST));
  }

  @Test
  public void createPluginClassLoaderWithPrivilegedContainerAccess() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());

    MuleModule privilegedModule = mock(MuleModule.class);
    when(privilegedModule.getPrivilegedArtifacts()).thenReturn(singleton(PLUGIN_ARTIFACT_ID1));
    when(privilegedModule.getPrivilegedExportedPackages()).thenReturn(singleton(PRIVILEGED_PACKAGE));
    when(moduleRepository.getModules()).thenReturn(singletonList(privilegedModule));

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> empty());

    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(PRIVILEGED_PACKAGE),
               instanceOf(ContainerOnlyLookupStrategy.class));
  }

  @Test
  public void createsPluginClassLoaderWithPrivilegedPluginAccess() {
    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(singletonList(plugin2Descriptor));

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> of(plugin2Descriptor));

    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(PRIVILEGED_PACKAGE),
               instanceOf(DelegateOnlyLookupStrategy.class));
  }

  @Test(expected = ArtifactActivationException.class)
  public void pluginClassLoadersOfDependenciesMustAlreadyExist() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());

    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> of(plugin2Descriptor));
  }

  @Test
  public void createPluginClassLoaderWithExportedLocalPackage() {
    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());
    String pluginPackage = "plugin-package";

    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPackages(singleton(pluginPackage)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().withLocalPackages(singleton(pluginPackage))
            .dependingOn(singleton(pluginDependency)).build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> of(plugin2Descriptor));

    // The local package should have precedence over the ones exported by other artifacts
    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(pluginPackage),
               instanceOf(ChildOnlyLookupStrategy.class));
  }

  @Test
  public void createPluginClassLoaderWithIgnoredLocalPackages() {
    MuleModule muleModule = mock(MuleModule.class);
    final String package1Name = "module&plugin-package";
    final String package2Name = "org.mule.sdk.api.package";
    when(muleModule.getExportedPackages()).thenReturn(Stream.of(package1Name, package2Name).collect(toSet()));
    when(moduleRepository.getModules()).thenReturn(singletonList(muleModule));

    MuleDeployableArtifactClassLoader applicationClassLoader = getTestApplicationClassLoader(emptyList());

    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder()
            .withLocalPackages(Stream.of(package1Name, package2Name).collect(toSet())).build());

    final MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                     (apds, d) -> empty());

    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(package1Name),
               instanceOf(ContainerOnlyLookupStrategy.class));
    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(package2Name),
               instanceOf(ParentFirstLookupStrategy.class));
  }

  @Test
  public void createPluginClassLoaderWithCachedPlugin() {
    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2Descriptor.setClassLoaderModel(plugin2ClassLoaderModel);

    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(
                                                                                                       PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI())
        .build();
    plugin1Descriptor
        .setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build());

    MuleDeployableArtifactClassLoader domainClassLoader =
        getTestDomainClassLoader(emptyList());

    final MuleArtifactClassLoader plugin2ClassLoader = artifactClassLoaderResolver
        .createMulePluginClassLoader(domainClassLoader, plugin2Descriptor,
                                     (apds, d) -> empty());

    MuleArtifactClassLoader pluginClassLoader = artifactClassLoaderResolver.createMulePluginClassLoader(domainClassLoader,
                                                                                                        plugin1Descriptor,
                                                                                                        (apds,
                                                                                                         d) -> of(plugin2Descriptor),
                                                                                                        (ownerClassLoader,
                                                                                                         pluginDescriptor) -> {
                                                                                                          if (pluginDescriptor
                                                                                                              .getBundleDescriptor()
                                                                                                              .getArtifactId()
                                                                                                              .equals(plugin2Descriptor
                                                                                                                  .getBundleDescriptor()
                                                                                                                  .getArtifactId())) {
                                                                                                            return of(() -> plugin2ClassLoader);
                                                                                                          } else {
                                                                                                            return empty();
                                                                                                          }
                                                                                                        });

    assertThat(pluginClassLoader.getClassLoaderLookupPolicy().getPackageLookupStrategy(PRIVILEGED_PACKAGE),
               instanceOf(DelegateOnlyLookupStrategy.class));
  }

  private MuleDeployableArtifactClassLoader getTestDomainClassLoader(List<ArtifactPluginDescriptor> plugins) {
    return getTestDomainClassLoader(plugins, emptySet());
  }

  private MuleDeployableArtifactClassLoader getTestDomainClassLoader(List<ArtifactPluginDescriptor> plugins,
                                                                     Set<String> exportedPackages) {
    final String domainName = "custom-domain";
    DomainDescriptor descriptor = getTestDomainDescriptor(domainName);
    descriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, domainName));
    descriptor.setPlugins(new HashSet<>(plugins));
    descriptor.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(exportedPackages).build());

    return artifactClassLoaderResolver.createDomainClassLoader(descriptor);
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins) {
    return getTestApplicationClassLoader(plugins, emptySet());
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins,
                                                                          Set<String> exportedPackages) {
    final String applicationName = "app";
    final MuleDeployableArtifactClassLoader domainClassLoader = getTestDomainClassLoader(emptyList());

    ApplicationDescriptor descriptor = new ApplicationDescriptor(applicationName);
    descriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    descriptor.setPlugins(new HashSet<>(plugins));
    descriptor.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(exportedPackages).build());

    return artifactClassLoaderResolver.createApplicationClassLoader(descriptor, () -> domainClassLoader);
  }

  private DomainDescriptor getTestDomainDescriptor(String name) {
    DomainDescriptor descriptor = new DomainDescriptor(name);
    descriptor.setRedeploymentEnabled(false);
    descriptor.setArtifactLocation(artifactLocation.getRoot());
    return descriptor;
  }

  protected File createDomainDir(String domainFolder, String domain) {
    final File file = new File(muleHomeFolder, domainFolder + File.separator + domain);
    assertThat(file.mkdirs(), is(true));
    return file;
  }

}

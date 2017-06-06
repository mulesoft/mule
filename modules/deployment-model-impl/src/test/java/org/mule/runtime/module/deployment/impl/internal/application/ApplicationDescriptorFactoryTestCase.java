/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppConfigFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppPluginsFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.deployment.model.api.application.ApplicationDescriptor.DEFAULT_CONFIGURATION_RESOURCE_LOCATION;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import static org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.NULL_CLASSLOADER_MODEL;

import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Set;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static File echoTestJarFile;

  private static File getResourceFile(String resource) throws URISyntaxException {
    return new File(ApplicationDescriptorFactoryTestCase.class.getResource(resource).toURI());
  }

  public static final String APP_NAME = "testApp";
  public static final String JAR_FILE_NAME = "test.jar";

  @BeforeClass
  public static void beforeClass() throws URISyntaxException {
    echoTestJarFile = new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java"))
        .including(getResourceFile("/test-resource.txt"), "META-INF/MANIFEST.MF")
        .including(getResourceFile("/test-resource.txt"), "README.txt")
        .compile("echo.jar");
  }

  @Rule
  public SystemProperty repositoryLocation = new SystemProperty("muleRuntimeConfig.maven.repositoryLocation",
                                                                discoverProvider(ApplicationDescriptorFactoryTestCase.class
                                                                    .getClassLoader()).getLocalRepositorySuppliers()
                                                                        .environmentMavenRepositorySupplier().get()
                                                                        .getAbsolutePath());

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);
  private ArtifactPluginRepository applicationPluginRepository;

  @Before
  public void setUp() throws Exception {
    GlobalConfigLoader.reset();
    applicationPluginRepository = mock(ArtifactPluginRepository.class);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors()).thenReturn(emptyList());
  }

  @Test
  public void makesConfigFileRelativeToAppMuleFolder() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder(APP_NAME)
        .deployedWith("config.resources", "mule/config1.xml,mule/config2.xml").deployedWith("domain", "default");
    unzip(applicationFileBuilder.getArtifactFile(), getAppFolder(APP_NAME));

    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
                                         applicationPluginRepository,
                                         createDescriptorLoaderRepository());

    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    String config1Path = new File(getAppConfigFolder(APP_NAME), "mule/config1.xml").getAbsolutePath();
    String config2Path = new File(getAppConfigFolder(APP_NAME), "mule/config2.xml").getAbsolutePath();
    assertThat(desc.getAbsoluteResourcePaths().length, equalTo(2));
    assertThat(desc.getAbsoluteResourcePaths(), arrayContainingInAnyOrder(config1Path, config2Path));
  }

  private ServiceRegistryDescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());
  }

  @Test
  public void readsPlugin() throws Exception {
    File pluginDir = getAppPluginsFolder(APP_NAME);
    pluginDir.mkdirs();
    final File pluginFile =
        new ArtifactPluginFileBuilder("plugin").usingLibrary(echoTestJarFile.getAbsolutePath()).getArtifactFile();
    copyFile(pluginFile, new File(pluginDir, "plugin1.zip"));
    copyFile(pluginFile, new File(pluginDir, "plugin2.zip"));

    final ArtifactPluginDescriptorFactory pluginDescriptorFactory = mock(ArtifactPluginDescriptorFactory.class);

    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(pluginDescriptorFactory),
                                         applicationPluginRepository, createDescriptorLoaderRepository());
    final ArtifactPluginDescriptor expectedPluginDescriptor1 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor1.getName()).thenReturn("plugin1");
    when(expectedPluginDescriptor1.getClassLoaderModel()).thenReturn(NULL_CLASSLOADER_MODEL);
    final ArtifactPluginDescriptor expectedPluginDescriptor2 = mock(ArtifactPluginDescriptor.class);
    when(expectedPluginDescriptor2.getName()).thenReturn("plugin2");
    when(expectedPluginDescriptor2.getClassLoaderModel()).thenReturn(NULL_CLASSLOADER_MODEL);
    when(pluginDescriptorFactory.create(any())).thenReturn(expectedPluginDescriptor1)
        .thenReturn(expectedPluginDescriptor2);

    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    Set<ArtifactPluginDescriptor> plugins = desc.getPlugins();
    assertThat(plugins.size(), equalTo(2));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor1)));
    assertThat(plugins, hasItem(equalTo(expectedPluginDescriptor2)));
  }

  @Test
  public void readsSharedLibs() throws Exception {
    File sharedLibsFolder = MuleFoldersUtil.getAppSharedLibsFolder(APP_NAME);
    sharedLibsFolder.mkdirs();

    File sharedLibFile = new File(sharedLibsFolder, JAR_FILE_NAME);
    copyResourceAs(echoTestJarFile.getAbsolutePath(), sharedLibFile);

    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
                                         applicationPluginRepository, createDescriptorLoaderRepository());
    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[0]).getPath(),
               equalTo(getAppClassesFolder(APP_NAME).toString()));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(), equalTo(sharedLibFile.toString()));
    assertThat(desc.getClassLoaderModel().getExportedPackages(), contains("org.foo"));
    assertThat(desc.getClassLoaderModel().getExportedResources(), containsInAnyOrder("META-INF/MANIFEST.MF",
                                                                                     "README.txt"));
  }

  @Test
  public void readsRuntimeLibs() throws Exception {
    File libDir = MuleFoldersUtil.getAppLibFolder(APP_NAME);
    libDir.mkdirs();

    File libFile = new File(libDir, JAR_FILE_NAME);
    copyResourceAs("test-jar-with-resources.jar", libFile);

    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
                                         applicationPluginRepository, createDescriptorLoaderRepository());
    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[0]).getPath(), equalTo(getAppClassesFolder(APP_NAME).toString()));
    assertThat(toFile(desc.getClassLoaderModel().getUrls()[1]).getPath(), equalTo(libFile.toString()));
  }

  private void copyResourceAs(String resourceName, File destination) throws IOException {
    final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
    copy(sourcePlugin, new FileOutputStream(destination));
  }

  @Test
  public void applicationDescriptorFromJson() throws Exception {
    String appPath = "apps/no-dependencies";
    ApplicationDescriptor desc = createApplicationDescriptor(appPath);

    assertThat(desc.getMinMuleVersion(), is(new MuleVersion("4.0.0")));
    assertThat(desc.getConfigResources(), hasSize(1));
    assertThat(desc.getConfigResources().get(0), is(DEFAULT_CONFIGURATION_RESOURCE_LOCATION));

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();
    assertThat(classLoaderModel.getDependencies().isEmpty(), is(true));
    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(toFile(classLoaderModel.getUrls()[0]).getPath(),
               is(new File(getApplicationFolder(appPath), "classes").getAbsolutePath()));

    assertThat(classLoaderModel.getExportedPackages().isEmpty(), is(true));
    assertThat(classLoaderModel.getExportedResources().isEmpty(), is(true));
    assertThat(classLoaderModel.getDependencies().isEmpty(), is(true));
  }

  @Test
  public void applicationDescriptorFromJsonWithCustomConfigFiles() throws Exception {
    String appPath = "apps/custom-config-files";
    ApplicationDescriptor desc = createApplicationDescriptor(appPath);

    assertThat(desc.getConfigResources(), contains("mule/file1.xml", "mule/file2.xml"));
  }

  @Test
  public void classLoaderModelWithSingleDependency() throws Exception {
    ApplicationDescriptor desc = createApplicationDescriptor("apps/single-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies(), hasSize(1));
    BundleDependency commonsCollectionDependency = classLoaderModel.getDependencies().iterator().next();
    assertThat(commonsCollectionDependency, commonsColecctionDependencyMatcher());

    assertThat(classLoaderModel.getUrls().length, is(2));
    assertThat(asList(classLoaderModel.getUrls()), hasItem(commonsCollectionDependency.getBundleUri()));
  }

  @Test
  public void classLoaderModelWithPluginDependency() throws Exception {
    ApplicationDescriptor desc = createApplicationDescriptor("apps/plugin-dependency");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(1));
    assertThat(classLoaderModel.getDependencies(), hasItem(socketsPluginDependencyMatcher()));

    assertThat(classLoaderModel.getUrls().length, is(1));
    assertThat(asList(classLoaderModel.getUrls()), not(hasItem(classLoaderModel.getDependencies().iterator().next())));
  }

  @Test
  public void classLoaderModelWithPluginDependencyWithAnotherPlugin() throws Exception {
    ApplicationDescriptor desc = createApplicationDescriptor("apps/plugin-dependency-with-another-plugin");

    ClassLoaderModel classLoaderModel = desc.getClassLoaderModel();

    assertThat(classLoaderModel.getDependencies().size(), is(2));
    assertThat(classLoaderModel.getDependencies(), hasItems(httpPluginDependencyMatcher(), httpSocketsDependencyMatcher()));

    assertThat(classLoaderModel.getUrls().length, is(1));
    classLoaderModel.getDependencies().stream()
        .forEach(bundleDependency -> {
          assertThat(asList(classLoaderModel.getUrls()), not(hasItem(bundleDependency.getBundleUri())));
        });
  }

  private Matcher<BundleDependency> commonsColecctionDependencyMatcher() {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            !bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getArtifactId().equals("commons-collections") &&
            bundleDependency.getDescriptor().getGroupId().equals("commons-collections") &&
            bundleDependency.getDescriptor().getVersion().equals("3.2.2");
      }
    };
  }

  private Matcher<BundleDependency> socketsPluginDependencyMatcher() {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText("invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals("mule-sockets-connector") &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.connectors") &&
            bundleDependency.getDescriptor().getVersion().equals("1.0.0-SNAPSHOT");
      }
    };
  }

  private Matcher<BundleDependency> httpPluginDependencyMatcher() {
    return createConnectorMatcher("mule-http-connector");
  }

  private Matcher<BundleDependency> httpSocketsDependencyMatcher() {
    return createConnectorMatcher("mule-sockets-connector");
  }

  private Matcher<BundleDependency> createConnectorMatcher(String artifactId) {
    return new BaseMatcher<BundleDependency>() {

      @Override
      public void describeTo(Description description) {
        description.appendText(" invalid bundle configuration");
      }

      @Override
      public boolean matches(Object o) {
        if (!(o instanceof BundleDependency)) {
          return false;
        }

        BundleDependency bundleDependency = (BundleDependency) o;
        return bundleDependency.getScope().equals(COMPILE) &&
            bundleDependency.getDescriptor().getClassifier().isPresent() &&
            bundleDependency.getDescriptor().getClassifier().get().equals(MULE_PLUGIN_CLASSIFIER) &&
            bundleDependency.getDescriptor().getArtifactId().equals(artifactId) &&
            bundleDependency.getDescriptor().getGroupId().equals("org.mule.connectors") &&
            bundleDependency.getDescriptor().getVersion().equals("1.0.0-SNAPSHOT");
      }
    };
  }

  private ApplicationDescriptor createApplicationDescriptor(String appPath) throws URISyntaxException {
    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(new ArtifactPluginDescriptorLoader(new ArtifactPluginDescriptorFactory()),
                                         applicationPluginRepository, createDescriptorLoaderRepository());

    return applicationDescriptorFactory.create(getApplicationFolder(appPath));
  }

  private File getApplicationFolder(String appPath) throws URISyntaxException {
    return new File(getClass().getClassLoader().getResource(appPath).toURI());
  }

  @Test
  public void readFromJsonDescriptor() throws Exception {

  }
}

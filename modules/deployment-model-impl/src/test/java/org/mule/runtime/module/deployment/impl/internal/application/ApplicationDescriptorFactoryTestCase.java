/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.IOUtils.copy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppClassesFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppPluginsFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.NULL_CLASSLOADER_MODEL;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ApplicationDescriptorFactoryTestCase extends AbstractMuleTestCase {

  private static final File echoTestJarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java"))
          .including(getResourceFile("/test-resource.txt"), "META-INF/MANIFEST.MF")
          .including(getResourceFile("/test-resource.txt"), "README.txt")
          .compile("echo.jar");

  private static File getResourceFile(String resource) {
    return new File(ApplicationDescriptorFactoryTestCase.class.getResource(resource).getFile());
  }

  public static final String APP_NAME = "testApp";
  public static final String JAR_FILE_NAME = "test.jar";

  @Rule
  public TemporaryFolder muleHome = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);
  private ArtifactPluginRepository applicationPluginRepository;

  @Before
  public void setUp() throws Exception {
    applicationPluginRepository = mock(ArtifactPluginRepository.class);
    when(applicationPluginRepository.getContainerArtifactPluginDescriptors()).thenReturn(emptyList());
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
                                         applicationPluginRepository);
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
                                         applicationPluginRepository);
    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(desc.getClassLoaderModel().getUrls()[0].getFile(),
               equalTo(getAppClassesFolder(APP_NAME).toString()));
    assertThat(desc.getClassLoaderModel().getUrls()[1].getFile(), equalTo(sharedLibFile.toString()));
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
                                         applicationPluginRepository);
    ApplicationDescriptor desc = applicationDescriptorFactory.create(getAppFolder(APP_NAME));

    assertThat(desc.getClassLoaderModel().getUrls().length, equalTo(2));
    assertThat(desc.getClassLoaderModel().getUrls()[0].getFile(), equalTo(getAppClassesFolder(APP_NAME).toString()));
    assertThat(desc.getClassLoaderModel().getUrls()[1].getFile(), equalTo(libFile.toString()));
  }

  private void copyResourceAs(String resourceName, File destination) throws IOException {
    final InputStream sourcePlugin = IOUtils.getResourceAsStream(resourceName, getClass());
    copy(sourcePlugin, new FileOutputStream(destination));
  }
}

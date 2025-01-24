/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.ClassloadingIsolationStory.CLASSLOADER_GENERATION;

import static java.lang.System.getProperty;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThrows;

import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.CompilerUtils.JarCompiler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CLASSLOADING_ISOLATION)
@Story(CLASSLOADER_GENERATION)
public class DefaultMuleClassPathConfigTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static TemporaryFolder folder = new TemporaryFolder();

  private static File muleHome;

  private ClassLoader containerClassLoader;

  private Class fromMuleModule;
  private Class fromThirdPartyLib;
  private Class fromMuleJavaSpecificModule;
  private Class fromThirdPartyJavaSpecificLib;

  @BeforeClass
  public static void setUpClass() throws Exception {
    File fromThirdPartyLibJarFile = new JarFileBuilder("third-party-lib",
                                                       new JarCompiler()
                                                           .compiling(getResourceFile("/org/test/opt/FromThirdPartyLib.java"))
                                                           .compile("third-party-lib.jar"))
                                                               .getArtifactFile();

    File fromMuleModuleJarFile = new JarFileBuilder("mule-module",
                                                    new JarCompiler()
                                                        .compiling(getResourceFile("/org/test/mule/FromMuleModule.java"))
                                                        .dependingOn(fromThirdPartyLibJarFile)
                                                        .compile("mule-module.jar"))
                                                            .getArtifactFile();

    File fromUserLibJarFile = new JarFileBuilder("user-lib",
                                                 new JarCompiler()
                                                     .compiling(getResourceFile("/org/test/user/FromUserLib.java"))
                                                     .compile("user-lib.jar"))
                                                         .getArtifactFile();

    File fromUserLibPropertiesFile = getResourceFile("/org/test/user/properties.yaml");

    muleHome = folder.newFolder("mule_home");

    copyFileToDirectory(fromMuleModuleJarFile, new File(muleHome, "lib/mule"));
    copyFileToDirectory(fromThirdPartyLibJarFile, new File(muleHome, "lib/opt"));
    copyFileToDirectory(fromUserLibJarFile, new File(muleHome, "lib/user"));
    copyFileToDirectory(fromUserLibPropertiesFile, new File(muleHome, "lib/user"));

    File fromThirdPartyLibJarJavaSpecificFile = new JarFileBuilder("third-party-lib-javaspecific",
                                                                   new JarCompiler()
                                                                       .compiling(getResourceFile("/org/test/opt/javaspecific/FromThirdPartyLib.java"))
                                                                       .compile("third-party-lib-javaspecific.jar"))
                                                                           .getArtifactFile();

    File fromMuleModuleJarJavaSpecificFile = new JarFileBuilder("mule-module-javaspecific",
                                                                new JarCompiler()
                                                                    .compiling(getResourceFile("/org/test/mule/javaspecific/FromMuleModule.java"))
                                                                    .dependingOn(fromThirdPartyLibJarJavaSpecificFile)
                                                                    .compile("mule-module-javaspecific.jar"))
                                                                        .getArtifactFile();

    final var javaSpecificationVersion = getProperty("java.specification.version").split("\\.")[0];
    copyFileToDirectory(fromMuleModuleJarJavaSpecificFile, new File(muleHome, "lib/mule/jdk-" + javaSpecificationVersion));
    copyFileToDirectory(fromThirdPartyLibJarJavaSpecificFile, new File(muleHome, "lib/opt/jdk-" + javaSpecificationVersion));
  }

  /**
   * Creates an instance of {@link File} with the path specified in the parameter {@code resource}.
   * 
   * @param resource the path to the file.
   * @return a {@link File} representing the resource.
   * @throws URISyntaxException if an error occurred while trying to convert the URI.
   */
  public static File getResourceFile(String resource) throws URISyntaxException {
    return new File(DefaultMuleClassPathConfigTestCase.class.getResource(resource).toURI());
  }

  @Before
  public void setUp() {
    final DefaultMuleClassPathConfig muleClassPathConfig = new DefaultMuleClassPathConfig(muleHome, muleHome);

    containerClassLoader = new AbstractMuleContainerFactory(null, null) {

      @Override
      protected DefaultMuleClassPathConfig createMuleClassPathConfig(File muleHome, File muleBase) {
        return new DefaultMuleClassPathConfig(muleHome, muleHome);
      }

    }.createContainerSystemClassLoader(muleHome, muleHome);

    try {
      fromMuleModule = containerClassLoader.loadClass("org.test.mule.FromMuleModule");
      fromThirdPartyLib = containerClassLoader.loadClass("org.test.opt.FromThirdPartyLib");
      fromMuleJavaSpecificModule = containerClassLoader.loadClass("org.test.mule.javaspecific.FromMuleModule");
      fromThirdPartyJavaSpecificLib = containerClassLoader.loadClass("org.test.opt.javaspecific.FromThirdPartyLib");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void muleOptClassLoaderVisibility() throws Exception {
    assertThat(fromMuleModule.getDeclaredMethod("useThirdPartyLib", null)
        .invoke(null),
               is("OK"));
  }

  @Test
  @Issue("W-16010357")
  public void optUserClassLoaderVisibility() throws Exception {
    assertThat(fromThirdPartyLib.getDeclaredMethod("useLib", String.class)
        .invoke(null, "org.test.user.FromUserLib"),
               is("OK"));
  }

  @Test
  public void optMuleNoClassLoaderVisibility() throws Exception {
    final String muleClassName = "org.test.mule.FromMuleModule";
    final Method useLibMethod = fromThirdPartyLib.getDeclaredMethod("useLib", String.class);

    var thrown = assertThrows(Exception.class, () -> useLibMethod.invoke(null, muleClassName));
    assertThat(thrown.getCause().getCause(), instanceOf(ClassNotFoundException.class));
    assertThat(thrown.getCause().getCause().getMessage(), containsString(muleClassName));
  }

  @Test
  public void muleOptJavaSpecificClassLoaderVisibility() throws Exception {
    assertThat(fromMuleJavaSpecificModule.getDeclaredMethod("useThirdPartyLib", null)
        .invoke(null),
               is("OK"));
  }

  @Test
  @Issue("W-16010357")
  public void optJavaSpoecificUserClassLoaderVisibility() throws Exception {
    assertThat(fromThirdPartyJavaSpecificLib.getDeclaredMethod("useLib", String.class)
        .invoke(null, "org.test.user.FromUserLib"),
               is("OK"));
  }

  @Test
  public void optMuleJavaSpecificNoClassLoaderVisibility() throws Exception {
    final String muleClassName = "org.test.mule.javaspecific.FromMuleModule";
    final Method useLibMethod = fromThirdPartyJavaSpecificLib.getDeclaredMethod("useLib", String.class);

    var thrown = assertThrows(Exception.class, () -> useLibMethod.invoke(null, muleClassName));
    assertThat(thrown.getCause().getCause(), instanceOf(ClassNotFoundException.class));
    assertThat(thrown.getCause().getCause().getMessage(), containsString(muleClassName));
  }

  @Test
  @Issue("W-16188357")
  public void findUserResource() {
    assertThat(containerClassLoader.getResource("properties.yaml"), notNullValue());
  }

}

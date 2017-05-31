/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.container.internal;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mule.runtime.container.internal.ExportedServiceMatcher.like;
import org.mule.runtime.module.artifact.classloader.ExportedService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class JreExplorerTestCase extends AbstractMuleTestCase {

  private static final String FOO_SERVICE_PATH = "META-INF/services/org.foo.FooService";
  private static final String SERVICE_RESOURCE = "META-INF/fooResource.txt";
  private static final String FOO_RESOURCE = "fooResource.txt";
  private static final String FOO_JAR_FILENAME = "foo.jar";
  private static final String FOO_SERVICE_INTERFACE = "org.foo.FooService";
  private static final String BAR_SERVICE_INTERFACE = "org.bar.BarService";
  private static final String FOO_PACKAGE = "org.foo";
  private static final String BAR_PACKAGE = "org.bar";
  private static final String LIB_FOLDER = "lib";
  private static final String SUB_FOLDER = "subFolder";
  private static final String BAR_SERVICE_PATH = "META-INF/services/org.bar.BarService";
  private static final String BAR_RESOURCE = "barResource.txt";
  private static final String BAR_JAR_FILENAMEß = "bar.jar";
  private static final String RESOURCE_PATH = "/resource.txt";


  private static final File fooJar = new CompilerUtils.JarCompiler()
      .compiling(new File(JreExplorerTestCase.class.getResource("/org/foo/Foo.java").getFile()))
      .including(new File(JreExplorerTestCase.class.getResource(RESOURCE_PATH).getFile()), FOO_RESOURCE)
      .including(new File(JreExplorerTestCase.class.getResource(RESOURCE_PATH).getFile()), SERVICE_RESOURCE)
      .including(new File(JreExplorerTestCase.class.getResource(RESOURCE_PATH).getFile()), FOO_SERVICE_PATH)
      .compile(FOO_JAR_FILENAME);

  private static final File barJar = new CompilerUtils.JarCompiler()
      .compiling(new File(JreExplorerTestCase.class.getResource("/org/bar/Bar.java").getFile()))
      .including(new File(JreExplorerTestCase.class.getResource(RESOURCE_PATH).getFile()), BAR_RESOURCE)
      .including(new File(JreExplorerTestCase.class.getResource(RESOURCE_PATH).getFile()), BAR_SERVICE_PATH)
      .compile(BAR_JAR_FILENAMEß);

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void readsJar() throws Exception {

    File libFolder = temporaryFolder.newFolder(LIB_FOLDER);
    File innerFooJar = new File(libFolder, FOO_JAR_FILENAME);
    copyFile(fooJar, innerFooJar);

    List<String> paths = Collections.singletonList(libFolder.getAbsolutePath());

    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    List<ExportedService> services = new ArrayList<>();
    JreExplorer.explorePaths(paths, packages, resources, services);

    assertThat(packages, contains(FOO_PACKAGE));
    assertThat(resources, containsInAnyOrder(FOO_RESOURCE, SERVICE_RESOURCE));
    assertThat(services, contains(like(FOO_SERVICE_INTERFACE, getServiceResourceUrl(innerFooJar, FOO_SERVICE_PATH))));
  }

  @Test
  public void readsJarInFolder() throws Exception {

    File libFolder = temporaryFolder.newFolder(LIB_FOLDER);
    File subFolder = new File(libFolder, SUB_FOLDER);
    File innerFooJar = new File(subFolder, FOO_JAR_FILENAME);
    copyFile(fooJar, innerFooJar);

    List<String> paths = Collections.singletonList(libFolder.getAbsolutePath());

    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    List<ExportedService> services = new ArrayList<>();
    JreExplorer.explorePaths(paths, packages, resources, services);

    assertThat(packages, contains(FOO_PACKAGE));
    assertThat(resources, containsInAnyOrder(FOO_RESOURCE, SERVICE_RESOURCE));
    assertThat(services, contains(like(FOO_SERVICE_INTERFACE, getServiceResourceUrl(innerFooJar, FOO_SERVICE_PATH))));
  }

  @Test
  public void multipleJars() throws Exception {
    File libFolder = temporaryFolder.newFolder(LIB_FOLDER);
    File subFolder = new File(libFolder, SUB_FOLDER);
    File innerFooJar = new File(libFolder, FOO_JAR_FILENAME);
    copyFile(fooJar, innerFooJar);
    File innerBarJar = new File(subFolder, BAR_JAR_FILENAMEß);
    copyFile(barJar, innerBarJar);

    List<String> paths = Collections.singletonList(libFolder.getAbsolutePath());

    Set<String> packages = new HashSet<>();
    Set<String> resources = new HashSet<>();
    List<ExportedService> services = new ArrayList<>();
    JreExplorer.explorePaths(paths, packages, resources, services);

    assertThat(packages, containsInAnyOrder(FOO_PACKAGE, BAR_PACKAGE));
    assertThat(resources, containsInAnyOrder(FOO_RESOURCE, SERVICE_RESOURCE, BAR_RESOURCE));
    assertThat(services, containsInAnyOrder(like(FOO_SERVICE_INTERFACE, getServiceResourceUrl(innerFooJar, FOO_SERVICE_PATH)),
                                            like(BAR_SERVICE_INTERFACE, getServiceResourceUrl(innerBarJar, BAR_SERVICE_PATH))));
  }

  private URL getServiceResourceUrl(File resourceFile, String serviceInterface) throws MalformedURLException {
    return JreExplorer.getServiceResourceUrl(resourceFile.toURI().toURL(), serviceInterface);
  }
}

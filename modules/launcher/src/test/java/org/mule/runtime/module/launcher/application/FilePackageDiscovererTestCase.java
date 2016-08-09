/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import org.mule.tck.ZipUtils;
import org.mule.tck.ZipUtils.ZipResource;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.runtime.core.util.FileUtils;

import java.io.File;
import java.util.Set;

import org.junit.Test;

public class FilePackageDiscovererTestCase extends AbstractMuleTestCase {

  private final FilePackageDiscoverer packageExplorer = new FilePackageDiscoverer();

  @Test
  public void readsPackagesFromJar() throws Exception {
    final ZipResource fooClass = new ZipResource("org/foo/EchoTest.clazz", "org/foo/Foo.class");
    final ZipResource barClass = new ZipResource("org/foo/EchoTest.clazz", "org/bar/Bar.class");
    final ZipResource[] zipResources = {fooClass, barClass};

    final File jarFile = File.createTempFile("test", ".jar");
    jarFile.delete();
    ZipUtils.compress(jarFile, zipResources);

    final Set<String> packages = packageExplorer.findPackages(jarFile.toURI().toURL());
    assertThat(packages.size(), equalTo(2));
    assertThat(packages, hasItem("org.foo"));
    assertThat(packages, hasItem("org.bar"));
  }

  @Test
  public void readsPackagesFromFolder() throws Exception {
    final File folder = File.createTempFile("test", "");
    folder.delete();
    folder.mkdirs();
    final File orgFolder = new File(folder, "org");
    final File orgFooFolder = new File(orgFolder, "foo");
    final File orgFooBarFolder = new File(orgFolder, "bar");
    FileUtils.writeStringToFile(new File(orgFooFolder, "Foo.class"), "foo");
    FileUtils.writeStringToFile(new File(orgFooBarFolder, "Bar.class"), "bar");

    final Set<String> packages = packageExplorer.findPackages(folder.toURI().toURL());
    assertThat(packages.size(), equalTo(2));
    assertThat(packages, hasItem("org.foo"));
    assertThat(packages, hasItem("org.bar"));
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.MuleUrlStreamHandlerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

//TODO MULE-10785 add tests for all the invalid cases for the muleplugin protocol
//TODO MULE-10785 add tests for classloaders mixing URLs, test for invalid elements, reassembly the classloader and look for the element again,etc
public class MulePluginUrlStreamHandlerTestCase {

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private static final String MULE_MODULE_PLUGIN = "mule-module-plugin";
  private URL zipLocation;

  @Before
  public void setUp() throws MalformedURLException {
    MulePluginUrlStreamHandler.register();
    MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory();
    String pluginPath =
        getClass().getClassLoader().getResource(new File(MULE_MODULE_PLUGIN).getPath()).getFile();
    File zippedFile = PluginZipUtils.zipDirectory(pluginPath, folder, MULE_MODULE_PLUGIN);

    zipLocation = zippedFile.toURI().toURL();
  }

  @Test
  public void testReadRootFile() throws IOException {
    String pluginProperties = "plugin.properties";

    URL url = getURL(pluginProperties);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    String pluginPropertiesActual = IOUtils.toString(actualInputStream);
    String expectedPluginProperties = IOUtils.toString(getClass().getClassLoader()
        .getResource(new File(MULE_MODULE_PLUGIN + File.separator + pluginProperties).getPath()));
    assertThat(pluginPropertiesActual, is(expectedPluginProperties));
  }

  @Test
  public void testReadFileUnderClasses() throws IOException {
    String clazz = "classes"
        + MulePluginURLConnection.SEPARATOR
        + "org/foo/echo/SomeClass.txt";

    URL url = getURL(clazz);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    String pluginPropertiesActual = IOUtils.toString(actualInputStream);
    assertThat(pluginPropertiesActual, is("content of some java class"));
  }

  @Test
  public void testReadRootLibJarElement() throws IOException {
    String file = "lib/test-jar-with-resources.jar";

    URL url = getURL(file);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    InputStream expectedInputStream =
        getClass().getClassLoader().getResourceAsStream(new File(MULE_MODULE_PLUGIN + File.separator + file).getPath());
    assertThat(IOUtils.contentEquals(actualInputStream, expectedInputStream), is(true));
  }

  @Test
  public void testReadElementWithinJar() throws IOException {
    String file = "lib/test-jar-with-resources.jar"
        + MulePluginURLConnection.SEPARATOR
        + "test-resource-2.txt";

    URL url = getURL(file);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    String actualContent = IOUtils.toString(actualInputStream);
    assertThat(actualContent, is("Just some text"));
  }

  @Test
  public void testClassloaderWithMulePluginUrls() throws IOException {
    URL classesURL = getURL("classes" + MulePluginURLConnection.SEPARATOR);
    URL jarURL = getURL("lib/test-jar-with-resources.jar" + MulePluginURLConnection.SEPARATOR);

    URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {classesURL, jarURL});

    //looking for resource that's located in the classesURL
    InputStream actualSomeClassInputStream = urlClassLoader.getResourceAsStream("org/foo/echo/SomeClass.txt");
    assertThat(actualSomeClassInputStream, notNullValue());
    assertThat(IOUtils.toString(actualSomeClassInputStream), is("content of some java class"));

    //looking for resource that's located in the jarURL (zip within zip scenario)
    InputStream testResourceWithinZipInputStream = urlClassLoader.getResourceAsStream("test-resource-2.txt");
    assertThat(testResourceWithinZipInputStream, notNullValue());
    assertThat(IOUtils.toString(testResourceWithinZipInputStream), is("Just some text"));
  }

  private URL getURL(String file) throws MalformedURLException {
    return new URL(MulePluginUrlStreamHandler.PROTOCOL, "", -1,
                   zipLocation + MulePluginURLConnection.SEPARATOR + file);
  }
}

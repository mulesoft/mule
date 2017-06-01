/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader.net;

import static java.io.File.separator;
import static org.apache.commons.io.IOUtils.contentEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.module.artifact.classloader.net.MuleArtifactUrlConnection.CLASSES_FOLDER;
import static org.mule.runtime.module.artifact.classloader.net.MuleArtifactUrlConnection.SEPARATOR;
import static org.mule.runtime.module.artifact.classloader.net.MuleArtifactUrlStreamHandler.PROTOCOL;
import static org.mule.runtime.module.artifact.classloader.net.MuleArtifactUrlStreamHandler.register;
import static org.mule.runtime.module.artifact.classloader.net.MuleUrlStreamHandlerFactory.installUrlStreamHandlerFactory;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests urls on a ZIP file that imitates a mule plugin successfully.
 *
 * @since 4.0
 */
@SmallTest
@RunWith(Parameterized.class)
public class MuleArtifactUrlStreamHandlerTestCase extends AbstractMuleTestCase {

  private static final String MULE_MODULE_ARTIFACT_PLUGIN = "mule-module-plugin";

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Parameterized.Parameter
  public String pluginFolder;

  private URL zipLocation;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"mule-artifact-folder"}, {"mule artifact folder with spaces"}});
  }

  @Before
  public void setUp() throws IOException {
    installUrlStreamHandlerFactory();
    register();
    String pluginPath = getClass().getClassLoader().getResource(new File(MULE_MODULE_ARTIFACT_PLUGIN).getPath()).getFile();

    File zipFile = folder.newFile(pluginFolder.concat(".zip"));
    compressFolder(new File(pluginPath).toPath(), zipFile);

    zipLocation = zipFile.toURI().toURL();
  }

  @Test
  public void testReadRootFile() throws IOException {
    String rootResource = "root-resource.txt";

    URL url = getURL(rootResource);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    String expectedRootResourceContent = org.apache.commons.io.IOUtils.toString(getClass().getClassLoader()
        .getResource(
                     new File(MULE_MODULE_ARTIFACT_PLUGIN + separator + rootResource)
                         .getPath()));
    assertThat(IOUtils.toString(actualInputStream), is(expectedRootResourceContent));
  }

  @Test
  public void testReadResourceUnderClasses() throws IOException {
    String resourceUnderClasses = CLASSES_FOLDER + SEPARATOR + "org/foo/echo/aResource.txt";

    URL url = getURL(resourceUnderClasses);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    String pluginPropertiesActual = IOUtils.toString(actualInputStream);
    assertThat(pluginPropertiesActual, is("content of some resource"));
  }

  @Test
  public void testReadRootLibJarElement() throws IOException {
    String file = "lib/test-jar-with-resources.jar";

    URL url = getURL(file);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    InputStream expectedInputStream =
        getClass().getClassLoader().getResourceAsStream(new File(MULE_MODULE_ARTIFACT_PLUGIN + separator + file).getPath());
    assertThat(contentEquals(actualInputStream, expectedInputStream), is(true));
  }

  @Test
  public void testReadElementWithinJar() throws IOException {
    String file = "lib/test-jar-with-resources.jar"
        + SEPARATOR
        + "test-resource-2.txt";

    URL url = getURL(file);
    InputStream actualInputStream = url.openStream();
    assertThat(actualInputStream, notNullValue());

    assertThat(IOUtils.toString(actualInputStream), is("Just some text"));
  }

  @Test(expected = MalformedURLException.class)
  public void testHttpUnsupportedProtocol() throws IOException {
    String file = "http://some.domain.com/artifact.zip"
        + SEPARATOR
        + "classes"
        + SEPARATOR
        + "some-non-existing-path.txt";

    URL urlWithMuleArtifactProtocol = new URL(PROTOCOL, "", -1, file);
    urlWithMuleArtifactProtocol.openStream();
  }

  @Test
  public void testClassloaderWithMulePluginUrls() throws IOException {
    URL classesURL = getURL(CLASSES_FOLDER + SEPARATOR);
    URL jarURL = getURL("lib/test-jar-with-resources.jar" + SEPARATOR);

    URLClassLoader urlClassLoader = new URLClassLoader(new URL[] {classesURL, jarURL});

    //looking for resource that's located in the classesURL
    InputStream actualSomeClassInputStream = urlClassLoader.getResourceAsStream("org/foo/echo/aResource.txt");
    assertThat(actualSomeClassInputStream, notNullValue());
    assertThat(IOUtils.toString(actualSomeClassInputStream), is("content of some resource"));

    //looking for resource that's located in the jarURL (zip within zip scenario)
    InputStream testResourceWithinZipInputStream = urlClassLoader.getResourceAsStream("test-resource-2.txt");
    assertThat(testResourceWithinZipInputStream, notNullValue());
    assertThat(IOUtils.toString(testResourceWithinZipInputStream), is("Just some text"));
  }

  private URL getURL(String file) throws MalformedURLException {
    return new URL(PROTOCOL, "", -1, zipLocation + SEPARATOR + file);
  }

  /**
   * Helper method that takes a {@code folderToCompress} path, targeting a directory, and compress every folder and
   * file in it into the desired {@code compressedFile} location.
   *
   * @param folderToCompress directory to compress
   * @param compressedFile   expected file to be write while compressing the folder
   * @throws IOException if there was an error writing the ZIP
   */
  private void compressFolder(final Path folderToCompress, final File compressedFile) throws IOException {
    try (
        FileOutputStream fos = new FileOutputStream(compressedFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {
      Files.walkFileTree(folderToCompress, new SimpleFileVisitor<Path>() {

        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          zos.putNextEntry(new ZipEntry(folderToCompress.relativize(file).toString()));
          Files.copy(file, zos);
          zos.closeEntry();
          return FileVisitResult.CONTINUE;
        }

        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          zos.putNextEntry(new ZipEntry(folderToCompress.relativize(dir).toString() + "/"));
          zos.closeEntry();
          return FileVisitResult.CONTINUE;
        }
      });
    }
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.util.IOUtils;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class MuleCustomEntityResolverTestCase {

  private static final int TIMEOUT = 6000;

  private static final String MULE_FAKE_CORE = "http://www.mulesoft.org/schema/mule/core/current/mule-fake-core.xsd";
  private static final String JAR_NAME = "stuff.jar";
  private static final String MULE_SCHEMAS = "META-INF/mule.schemas";
  private static final String MULE_SCHEMAS_CONTENT =
      "http\\://www.mulesoft.org/schema/mule/core/current/mule-fake-core.xsd=META-INF/mule-fake-core.xsd";
  private static final String MULE_FAKE_CORE_XSD_FILE_NAME = "META-INF/mule-fake-core.xsd";
  private static final String MULE_FAKE_CONTENT = "schema-content";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  @Issue("MULE-18812")
  public void loadSchemasResourcesEvenWhenJarFileIsClosed() throws Exception {
    File jarFile = createDummyJar();
    URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()});

    final Latch latch = new Latch();
    MuleCustomEntityResolver entityResolver = new MuleCustomEntityResolver(classLoader);
    InputSource is = entityResolver.resolveEntity(null, MULE_FAKE_CORE);
    assertThat(is, is(notNullValue()));

    Thread thread = new Thread(() -> {
      try {
        // Force jar close
        JarURLConnection urlConnection =
            (JarURLConnection) classLoader.getResource(MULE_FAKE_CORE_XSD_FILE_NAME).openConnection();
        urlConnection.getJarFile().close();
      } catch (IOException e) {
        fail("Unexpected exception was caught when trying to close jar file");
      }
      latch.release();
    });
    thread.start();
    latch.await(TIMEOUT, MILLISECONDS);
    assertThat(IOUtils.toString(is.getByteStream()), is(MULE_FAKE_CONTENT));
  }

  @Test
  @Issue("MULE-18812")
  public void loadSchemasResourcesEvenWhenUrlClassLoaderIsClosedWhileInputStreamIsBeingUsed() throws Exception {
    File jarFile = createDummyJar();
    URLClassLoader cl = new URLClassLoader(new URL[] {jarFile.toURI().toURL()});
    URLClassLoader cl2 = new URLClassLoader(new URL[] {jarFile.toURI().toURL()});

    final Latch latch = new Latch();
    MuleCustomEntityResolver entityResolver = new MuleCustomEntityResolver(cl);
    InputSource is = entityResolver.resolveEntity(null, MULE_FAKE_CORE);
    assertThat(is, is(notNullValue()));

    Thread thread = new Thread(() -> {
      try {
        // Classloader close
        InputStream inputStream = cl2.getResourceAsStream(MULE_FAKE_CORE_XSD_FILE_NAME);
        assertThat(inputStream, is(notNullValue()));
        assertThat(IOUtils.toString(inputStream), is(MULE_FAKE_CONTENT));
        cl2.close();
      } catch (IOException e) {
        fail("Unexpected exception was caught when trying to close jar file");
      }
      latch.release();
    });
    thread.start();
    latch.await(TIMEOUT, MILLISECONDS);
    assertThat(IOUtils.toString(is.getByteStream()), is(MULE_FAKE_CONTENT));
  }

  private File createDummyJar() throws IOException {
    File jarFile = temporaryFolder.newFile(JAR_NAME);
    try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile))) {
      jar.putNextEntry(new JarEntry(MULE_SCHEMAS));
      jar.write(MULE_SCHEMAS_CONTENT.getBytes());
      jar.closeEntry();
      jar.putNextEntry(new JarEntry(MULE_FAKE_CORE_XSD_FILE_NAME));
      jar.write(MULE_FAKE_CONTENT.getBytes());
      jar.closeEntry();
    }
    return jarFile;
  }
}

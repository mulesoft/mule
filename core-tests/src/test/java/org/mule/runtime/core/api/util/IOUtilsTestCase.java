/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUFFER_SIZE;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.runtime.core.api.util.IOUtils.getInputStreamWithCacheControl;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.tck.mockito.plugins.ConfigurableMockitoPluginSwitch.disablePlugins;
import static org.mule.tck.mockito.plugins.ConfigurableMockitoPluginSwitch.enablePlugins;

import static java.io.File.pathSeparator;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.nio.charset.Charset.forName;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.apache.commons.lang3.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;
import org.mockito.internal.creation.bytebuddy.InlineByteBuddyMockMaker;
import org.mockito.invocation.InvocationOnMock;

@SmallTest
public class IOUtilsTestCase extends AbstractMuleTestCase {

  private static final List<String> POWER_MOCK_PLUGINS = asList("mock-maker-inline", InlineByteBuddyMockMaker.class.getName());

  private static final String JAR_NAME = "stuff.jar";
  private static final String RESOURCE_NAME = "SomeFile.xml";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @BeforeClass
  public static void setupInlineMockMaker() {
    enablePlugins(POWER_MOCK_PLUGINS);
  }

  @AfterClass
  public static void restoreInlineMockMaker() {
    disablePlugins(POWER_MOCK_PLUGINS);
  }

  @Test
  @Issue("MULE-18264")
  @Description("The URLConnection used to read a resource inside a jar shouldn't have cache enabled")
  public void cacheOnFalseWhenLoadResourceFromJar() throws Exception {
    File jarFile = createDummyJar();
    URLClassLoader classLoader = new URLClassLoader(new URL[] {jarFile.toURI().toURL()});

    URLConnection connection = mockURLConnection(classLoader.getResource(RESOURCE_NAME));

    assertThat(connection, is(instanceOf(JarURLConnection.class)));
    assertInputStream(connection);
    assertThat(connection.getUseCaches(), is(false));
    verify(connection, atLeastOnce()).setUseCaches(false);
  }


  @Test
  @Issue("MULE-18264")
  @Description("The URLConnection used to read a resource located in filesystem should have cache enabled")
  public void cacheOnTrueWhenLoadFromFilesystem() throws Exception {
    URLConnection connection = mockURLConnection(temporaryFolder.newFile(RESOURCE_NAME).toURI().toURL());

    assertInputStream(connection);
    verify(connection, never()).setUseCaches(false);
  }

  @Test
  public void testLoadingResourcesAsStream() throws Exception {
    InputStream is = getResourceAsStream("log4j2-test.xml", getClass(), false, false);
    assertNotNull(is);

    is = getResourceAsStream("does-not-exist.properties", getClass(), false, false);
    assertNull(is);
  }

  @Test
  public void bufferSize() throws Exception {
    InputStream in = new ByteArrayInputStream(new byte[8 * 1024]);
    OutputStream out = mock(OutputStream.class);

    IOUtils.copyLarge(in, out);

    // Default buffer size of 4KB required two reads to copy 8KB input stream
    verify(out, times(2)).write(any(byte[].class), anyInt(), anyInt());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void increaseBufferSizeViaSystemProperty() throws Exception {
    final int newBufferSize = 8 * 1024;

    testWithSystemProperty(MULE_STREAMING_BUFFER_SIZE, Integer.toString(newBufferSize), () -> {
      InputStream in = new ByteArrayInputStream(new byte[newBufferSize]);
      OutputStream out = mock(OutputStream.class);

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();

      String classPath = getProperty("java.class.path");
      String modulePath = getProperty("jdk.module.path");
      List<String> classPathEntries = (modulePath != null
          ? concat(Stream.of(classPath.split(pathSeparator)),
                   Stream.of(modulePath.split(pathSeparator)))
          : Stream.of(classPath.split(pathSeparator)))
          .filter(StringUtils::isNotBlank)
          .collect(toList());

      ClassLoader newClassLoader = new URLClassLoader(classPathEntries.stream().map(path -> {
        try {
          return get(path).toUri().toURL();
        } catch (MalformedURLException e) {
          fail(e.getMessage());
          return null;
        }
      })
          .toArray(s -> new URL[s]), null);
      Class clazz = loadClass(IOUtils.class.getCanonicalName(), newClassLoader);

      try {
        currentThread().setContextClassLoader(newClassLoader);
        clazz.getMethod("copyLarge", InputStream.class, OutputStream.class).invoke(clazz, in, out);
      } finally {
        currentThread().setContextClassLoader(contextClassLoader);
      }

      // With 8KB buffer define via system property only 1 read is required for 8KB
      // input stream
      verify(out, times(1)).write(any(byte[].class), anyInt(), anyInt());
    });
  }

  @Test
  public void convertsToStringWithEncoding() throws Exception {

    final Charset encoding = forName("EUC-JP");
    final String encodedText = "\u3053";
    InputStream in = new ByteArrayInputStream(encodedText.getBytes(encoding));

    String converted = IOUtils.toString(in, encoding);

    assertThat(converted, equalTo(encodedText));
  }

  private URLConnection mockURLConnection(URL url) throws Exception {
    try (MockedStatic<IOUtils> utilities = mockStatic(IOUtils.class)) {
      AtomicReference<URLConnection> connection = new AtomicReference<>();

      final URL mockedURL = spy(url);
      utilities.when(() -> getResourceAsStream(anyString(), any(Class.class)))
          .then(InvocationOnMock::callRealMethod);
      utilities.when(() -> getResourceAsStream(anyString(), any(Class.class), anyBoolean(), anyBoolean()))
          .then(InvocationOnMock::callRealMethod);
      utilities.when(() -> getInputStreamWithCacheControl(any(URL.class)))
          .then(InvocationOnMock::callRealMethod);
      utilities.when(() -> IOUtils.getResourceAsUrl(anyString(), any(Class.class), anyBoolean(), anyBoolean()))
          .then(invocationOnMock -> mockedURL);

      when(mockedURL.openConnection()).thenAnswer(a -> {
        URLConnection conn = spy((URLConnection) a.callRealMethod());
        connection.set(conn);
        return conn;
      });

      getResourceAsStream(RESOURCE_NAME, getClass());

      return connection.get();
    }
  }

  private void assertInputStream(URLConnection connection) throws Exception {
    try (InputStream is = connection.getInputStream()) {
      assertNotNull(is);
    }
  }

  private File createDummyJar() throws IOException {
    File jarFile = temporaryFolder.newFile(JAR_NAME);
    try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile))) {
      jar.putNextEntry(new JarEntry(RESOURCE_NAME));
    }
    return jarFile;
  }

}

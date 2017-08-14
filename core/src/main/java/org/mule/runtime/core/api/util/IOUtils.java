/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.apache.commons.lang3.math.NumberUtils.toInt;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.message.ds.ByteArrayDataSource;
import org.mule.runtime.core.api.message.ds.InputStreamDataSource;
import org.mule.runtime.core.api.message.ds.StringDataSource;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mule input/output utilities.
 */
public class IOUtils {

  private static final Log logger = LogFactory.getLog(IOUtils.class);

  protected static int bufferSize = toInt(System.getProperty(MuleProperties.MULE_STREAMING_BUFFER_SIZE), 4 * 1024);

  /**
   * Attempts to load a resource from the file system, from a URL, or from the classpath, in that order.
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @return the requested resource as a string
   * @throws java.io.IOException IO error
   */
  public static String getResourceAsString(final String resourceName, final Class callingClass) throws IOException {
    try (InputStream is = getResourceAsStream(resourceName, callingClass)) {
      if (is != null) {
        return toString(is);
      } else {
        throw new IOException("Unable to load resource " + resourceName);
      }
    }
  }

  /**
   * Attempts to load a resource from the file system, from a URL, or from the classpath, in that order.
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @return an InputStream to the resource or null if resource not found
   * @throws java.io.IOException IO error
   */
  public static InputStream getResourceAsStream(final String resourceName, final Class callingClass) throws IOException {
    return getResourceAsStream(resourceName, callingClass, true, true);
  }

  /**
   * Attempts to load a resource from the file system, from a URL, or from the classpath, in that order.
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @param tryAsFile - try to load the resource from the local file system
   * @param tryAsUrl - try to load the resource as a URL
   * @return an InputStream to the resource or null if resource not found
   * @throws java.io.IOException IO error
   */
  public static InputStream getResourceAsStream(final String resourceName, final Class callingClass, boolean tryAsFile,
                                                boolean tryAsUrl)
      throws IOException {

    URL url = getResourceAsUrl(resourceName, callingClass, tryAsFile, tryAsUrl);

    if (url == null) {
      return null;
    } else {
      return url.openStream();
    }
  }

  /**
   * Attempts to load a resource from the file system or from the classpath, in that order.
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @return an URL to the resource or null if resource not found
   */
  public static URL getResourceAsUrl(final String resourceName, final Class callingClass) {
    return getResourceAsUrl(resourceName, callingClass, true, true);
  }

  /**
   * Attempts to load a resource from the file system or from the classpath, in that order.
   *
   * @param resourceName The name of the resource to load
   * @param callingClass The Class object of the calling object
   * @param tryAsFile - try to load the resource from the local file system
   * @param tryAsUrl - try to load the resource as a Url string
   * @return an URL to the resource or null if resource not found
   */
  public static URL getResourceAsUrl(final String resourceName, final Class callingClass, boolean tryAsFile, boolean tryAsUrl) {
    if (resourceName == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("Resource name").getMessage());
    }
    URL url = null;

    // Try to load the resource from the file system.
    if (tryAsFile) {
      try {
        File file = FileUtils.newFile(resourceName);
        if (file.exists()) {
          url = file.getAbsoluteFile().toURL();
        } else {
          logger.debug("Unable to load resource from the file system: " + file.getAbsolutePath());
        }
      } catch (Exception e) {
        logger.debug("Unable to load resource from the file system: " + e.getMessage());
      }
    }

    // Try to load the resource from the classpath.
    if (url == null) {
      try {
        url = (URL) AccessController.doPrivileged((PrivilegedAction) () -> ClassUtils.getResource(resourceName, callingClass));
        if (url == null) {
          logger.debug("Unable to load resource " + resourceName + " from the classpath");
        }
      } catch (Exception e) {
        logger.debug("Unable to load resource " + resourceName + " from the classpath: " + e.getMessage());
      }
    }

    if (url == null) {
      try {
        url = new URL(resourceName);
      } catch (MalformedURLException e) {
        // ignore
      }
    }
    return url;
  }

  /**
   * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toString(InputStream)</code> method but catches any
   * {@link IOException} and wraps it into a {@link RuntimeException}.
   */
  public static String toString(InputStream input) {
    try {
      return org.apache.commons.io.IOUtils.toString(input);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /**
   * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toString(InputStream, Charset)</code> method but catches any
   * {@link IOException} and wraps it into a {@link RuntimeException}.
   */
  public static String toString(InputStream input, Charset encoding) {
    try {
      return org.apache.commons.io.IOUtils.toString(input, encoding);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /**
   * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toString(InputStream, String)</code> method but catches any
   * {@link IOException} and wraps it into a {@link RuntimeException}.
   */
  public static String toString(InputStream input, String encoding) {
    try {
      return org.apache.commons.io.IOUtils.toString(input, encoding);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /**
   * Similar to {@link #toByteArray(InputStream)} but obtaining the stream from the given
   * {@code cursorStreamProvider}
   */
  public static String toString(CursorStreamProvider cursorStreamProvider) {
    try (InputStream input = cursorStreamProvider.openCursor()) {
      return org.apache.commons.io.IOUtils.toString(input);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /**
   * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toByteArray(InputStream)</code> method but catches any
   * {@link IOException} and wraps it into a {@link RuntimeException}.
   */
  public static byte[] toByteArray(InputStream input) {
    try {
      return org.apache.commons.io.IOUtils.toByteArray(input);
    } catch (IOException iox) {
      throw new RuntimeException(iox);
    }
  }

  /**
   * This method wraps {@link org.apache.commons.io.IOUtils}' <code>toByteArray(InputStream)</code> method but catches any
   * {@link IOException} and wraps it into a {@link RuntimeException}.
   */
  public static byte[] toByteArray(CursorStreamProvider cursorStreamProvider) {
    try (InputStream input = cursorStreamProvider.openCursor()) {
      return toByteArray(input);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Re-implement copy method to allow buffer size to be configured. This won't impact all methods because there is no
   * polymorphism for static methods, but rather just direct use of these two methods.
   */
  public static long copyLarge(InputStream input, OutputStream output) throws IOException {
    return copyLarge(input, output, bufferSize);
  }

  /**
   * Re-implement copy method to allow buffer size to be configured. This won't impact all methods because there is no
   * polymorphism for static methods, but rather just direct use of these two methods.
   */
  public static long copyLarge(Reader input, Writer output) throws IOException {
    char[] buffer = new char[bufferSize];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Copies the data read from the {@link InputStream} into the {@link OutputStream}
   * using a buffer of size {@code bufferSize}
   */
  public static long copyLarge(InputStream input, OutputStream output, int bufferSize) throws IOException {
    byte[] buffer = new byte[bufferSize];
    long count = 0;
    int n = 0;
    while (-1 != (n = input.read(buffer))) {
      output.write(buffer, 0, n);
      count += n;
    }
    return count;
  }

  /**
   * Transforms an Object into a DataHandler of its corresponding type.
   *
   * @param name the name of the attachment being handled
   * @param object the attachment to be handled
   * @param contentType the Content-Type of the attachment that is being handled
   * @return a {@link DataHandler} of the corresponding attachment
   * @throws IOException if the transformation fails.
   */
  public static DataHandler toDataHandler(String name, Object object, MediaType contentType) throws IOException {
    DataHandler dh;
    if (object instanceof File) {
      if (contentType != null) {
        dh = new DataHandler(new FileInputStream((File) object), contentType.toString());
      } else {
        dh = new DataHandler(new FileDataSource((File) object));
      }
    } else if (object instanceof URL) {
      if (contentType != null) {
        dh = new DataHandler(((URL) object).openStream(), contentType.toString());
      } else {
        dh = new DataHandler((URL) object);
      }
    } else if (object instanceof String) {
      if (contentType != null) {
        dh = new DataHandler(new StringDataSource((String) object, name, contentType));
      } else {
        dh = new DataHandler(new StringDataSource((String) object, name));
      }
    } else if (object instanceof byte[] && contentType != null) {
      dh = new DataHandler(new ByteArrayDataSource((byte[]) object, contentType, name));
    } else if (object instanceof InputStream && contentType != null) {
      dh = new DataHandler(new InputStreamDataSource((InputStream) object, contentType, name));
    } else {
      dh = new DataHandler(object, contentType != null ? contentType.toString() : null);
    }
    return dh;
  }

  public static void ifInputStream(Object value, CheckedConsumer<InputStream> consumer) throws NotAnInputStreamException {
    ifInputStream(value, stream -> {
      consumer.accept(stream);
      return null;
    });
  }

  public static <T> T ifInputStream(Object value, CheckedFunction<InputStream, T> function) throws NotAnInputStreamException {
    boolean shouldCloseStream = false;
    InputStream stream = null;
    if (value instanceof CursorStreamProvider) {
      stream = ((CursorStreamProvider) value).openCursor();
      shouldCloseStream = true;
    } else if (value instanceof InputStream) {
      stream = (InputStream) value;
    } else {
      throw new NotAnInputStreamException(stream);
    }

    try {
      return function.apply(stream);
    } finally {
      if (shouldCloseStream) {
        closeQuietly(stream);
      }
    }
  }

  /**
   * Closes a {#link Closable} instance catching any exceptions
   *
   * @param closeable instance to be closed. Non null.
   */
  public static void closeQuietly(Closeable closeable) {
    org.apache.commons.io.IOUtils.closeQuietly(closeable);
  }
}

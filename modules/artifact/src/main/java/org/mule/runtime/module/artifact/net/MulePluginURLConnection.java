/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.net;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import sun.net.www.ParseUtil;

/**
 * A URL Connection to a Mule Plugin file or an entry in a Mule Plugin
 * file.
 *
 * <p>The syntax of a Mule Plugin URL is (current support for protocols under url is 'file' only):
 * <pre>
 * muleplugin:&lt;url&gt;!/{entry}
 * </pre>
 *
 * <p>valid samples:
 * <p>{@code muleplugin:file:/folder/mule-plugin.zip!/classes!/org/foo/echo/SomeClass.txt}
 * <p>{@code muleplugin:file:/folder/mule-plugin.zip!/lib/test-jar-with-resources.jar!/test-resource-2.txt}
 *
 * <p> invalid samples:
 * <p>{@code muleplugin:file:/folder/mule-plugin.zip}
 * <p>{@code muleplugin:file:/folder/mule-plugin.zip!/}
 * <p>{@code muleplugin:http:/folder/mule-plugin.zip!/classes/org/foo/echo/SomeClass.txt} (protocol is 'http', only 'file' supported)
 *
 * <p>Notice that after the URL targeting the zip file, there must be several separators '!/' elements, due to the fact
 * that this class is meant to open every zip until it finds out the expected file
 *
 * @since 4.0
 */
public class MulePluginURLConnection extends URLConnection {

  public static final String SEPARATOR = "!/";
  private ZipFile zipFile;
  private List<String> files;

  public MulePluginURLConnection(URL url) throws MalformedURLException {
    super(url);
    parseSpecs(url);
  }

  private void parseSpecs(URL url) throws MalformedURLException {
    String spec = url.getFile();
    int separator = spec.indexOf(SEPARATOR);
    if (separator == -1) {
      throw new MalformedURLException("no !/ found in url spec:" + spec);
    }
    URL mulePluginLocation = new URL(spec.substring(0, separator++));
    if (!"file".equals(mulePluginLocation.getProtocol())) {
      throw new MalformedURLException(format("supported protocol is 'file', but received '%s'",
                                             mulePluginLocation.getProtocol()));
    }
    try {
      zipFile = new ZipFile(ParseUtil.decode(mulePluginLocation.getFile()));
    } catch (IOException e) {
      throw new MalformedURLException(format("there was a problem opening a zip for %s", mulePluginLocation));
    }
    files = getFiles(spec.substring(++separator));
  }

  @Override
  public void connect() throws IOException {
    //does nothing for now
  }

  @Override
  public InputStream getInputStream() throws IOException {
    Deque<String> queue = new ArrayDeque<>(files);
    String filename = queue.pop();
    ZipEntry entry = zipFile.getEntry(filename);
    if (entry == null) {
      throw new MalformedURLException(format("file %s is missing in %s plugin", filename, zipFile.getName()));
    }
    InputStream is = zipFile.getInputStream(entry);
    if (!queue.isEmpty()) {
      //there are more files to look for, will work them recursively assuming each entry is either a zip until we get upto the file
      is = getInputStream(is, queue);
    }
    return is;
  }

  /**
   *  Recursively iterates the {@code files} queue to lookup for the element, ends successfully when it gets to the bottom of it
   *
   * @param currentStream position to the current element of the zip file (it moves in the recursion targeting the contents of a zip file)
   * @param files the queue with the files that has to be introspected
   * @return the input stream of the file that has been looked for
   * @throws IOException if the file is not present
   */
  private InputStream getInputStream(InputStream currentStream, Deque<String> files) throws IOException {
    if (files.isEmpty()) {
      return currentStream;
    }
    String expectedFile = files.pop();
    ZipInputStream zipInputStream = new ZipInputStream(currentStream);

    ZipEntry entry;
    while ((entry = zipInputStream.getNextEntry()) != null) {
      if (entry.getName().equals(expectedFile)) {
        return getInputStream(zipInputStream, files);
      }
    }
    throw new MalformedURLException(format("can't find the %s file in %s", expectedFile, zipFile.getName()));
  }

  private List<String> getFiles(final String file) {
    if (file.isEmpty()) {
      throw new IllegalArgumentException("there's no file to process after the !/");
    }
    String[] files = file.split(SEPARATOR);
    if ("classes".equals(files[0]) && files.length == 2) {
      return Arrays.asList("classes".concat("/").concat(files[1]));
    }
    return Arrays.stream(files)
        .map(ParseUtil::decode).collect(Collectors.toList());
  }
}

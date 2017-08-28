/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.net;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.lang3.StringUtils.endsWithIgnoreCase;

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
 * A URL Connection to a Mule Artifact file or an entry in a Mule Artifact file.
 *
 * <p>
 * The syntax of a Mule Artifact URL is (current support for protocols under URL are described in {@link #SUPPORTED_PROTOCOLS}):
 * 
 * <pre>
 * muleartifact:&lt;url&gt;!/{entry}
 * </pre>
 *
 * <p>
 * Where "url" is targeting a ZIP type of file to be decompressed, and the subsequent N-1 elements in "{entry}" are zips as well
 * and the N element is the file to look for. That means the {@link #getInputStream()} will open as many zips as needed to look
 * for the file.
 *
 * <p>
 * valid samples:
 * <p>
 * {@code muleartifact:file:/folder/mule-plugin.zip!/classes!/org/foo/echo/aResource.txt}
 * <p>
 * {@code muleartifact:file:/folder/mule-plugin.zip!/lib/test-jar-with-resources.jar!/test-resource-2.txt}
 *
 * <p>
 * invalid samples:
 * <p>
 * {@code muleartifact:file:/folder/mule-plugin.zip}
 * <p>
 * {@code muleartifact:file:/folder/mule-plugin.zip!/}
 * <p>
 * {@code muleartifact:http:/folder/mule-plugin.zip!/classes/org/foo/echo/aResource.txt} (protocol is 'http')
 *
 * <p>
 * Notice that after the URL targeting the ZIP file, there must be several separators '!/' elements, due to the fact that this
 * class is meant to open every ZIP until it finds out the expected file.
 *
 * TODO(fernandezlautaro): MULE-10892 at some moment this class should be strong enough to support any type of artifact.
 * 
 * @since 4.0
 */
public class MuleArtifactUrlConnection extends URLConnection {

  static final String SEPARATOR = "!/";
  private static final List<String> SUPPORTED_PROTOCOLS = asList("file");
  /**
   * initialized from the first element of the split URL, which must be a zip form of file
   */
  private ZipFile artifactZip;
  private List<String> files;

  /**
   * Takes an {@link URL} to validate its format in the {@link #connect()} ()} method, if there aren't any problem, it will store
   * the ZIP file in {@code artifactZip} and all the files that are accessible from that starting point.
   */
  public MuleArtifactUrlConnection(URL url) {
    super(url);
  }

  /**
   * Given the {@link URL} that was feed in the {@link #MuleArtifactUrlConnection(URL)} constructor, it will validate its format
   * through the {@link #parseSpecs()} method.
   * <p>
   * If there aren't any problem during validation, it will store a ZIP file in {@code artifactZip} and all the subsequent files
   * that are accessible from that starting point.
   *
   * @throws IOException if the first element is not a ZIP file, or if the protocol is not supported, or if it's impossible to
   *         create a {@link ZipFile} from the parsed {@code url}, or if there's not at least a {@link #SEPARATOR} in the
   *         {@code url}.
   */
  @Override
  public void connect() throws IOException {
    if (!connected) {
      parseSpecs();
      this.connected = true;
    }
  }

  /**
   * Returns an input stream that represents the element in the {@code url} from the farthest {@link #SEPARATOR} mark. For the
   * following {@link URL} samples:
   * <p>
   * {@code muleartifact:file:/folder/mule-plugin.zip!/classes!/org/foo/echo/aResource.txt}
   * <p>
   * {@code muleartifact:file:/folder/mule-plugin.zip!/lib/test-jar-with-resources.jar!/test-resource-2.txt}
   *
   * The expected input streams will be the content of "org/foo/echo/aResource.txt" and "test-resource-2.txt" respectively.
   *
   * @return an input stream that represents the element in the {@code url} from the farthest {@link #SEPARATOR} mark.
   * @throws IOException
   */
  @Override
  public InputStream getInputStream() throws IOException {
    connect();
    Deque<String> queue = new ArrayDeque<>(files);
    String filename = queue.pop();
    ZipEntry entry = artifactZip.getEntry(filename);
    if (entry == null) {
      throw new MalformedURLException(format("File '%s' is missing in '%s' artifact", filename, artifactZip.getName()));
    }
    InputStream is = artifactZip.getInputStream(entry);
    if (!queue.isEmpty()) {
      // there are more files to look for, will work them recursively assuming each entry is either a ZIP until we get up to the
      // file
      is = getInputStream(is, queue);
    }
    return is;
  }

  private void parseSpecs() throws MalformedURLException {
    String spec = url.getFile();
    int separator = seekFirstSeparator(spec);
    URL muleArtifactLocation = new URL(spec.substring(0, separator++));
    if (!SUPPORTED_PROTOCOLS.contains((muleArtifactLocation.getProtocol()))) {
      throw new MalformedURLException(format("Supported protocols for '%s' are '%s', but received '%s' (full URL received '%s')",
                                             MuleArtifactUrlStreamHandler.PROTOCOL,
                                             join(",", SUPPORTED_PROTOCOLS),
                                             muleArtifactLocation.getProtocol(),
                                             url.toString()));
    }
    try {
      artifactZip = new ZipFile(toFile(muleArtifactLocation));
    } catch (IOException e) {
      throw new MalformedURLException(format("There was a problem opening a zip for '%s'", muleArtifactLocation));
    }
    files = getFiles(spec.substring(++separator));
  }

  private int seekFirstSeparator(String spec) throws MalformedURLException {
    int separator = spec.indexOf(SEPARATOR);
    if (separator == -1) {
      throw new MalformedURLException(format("No separator '%s' found in url spec '%s'", SEPARATOR, spec));
    }
    return separator;
  }

  /**
   * Recursively iterates the {@code files} queue to lookup for the element, ends successfully when it gets to the bottom of it.
   *
   * @param currentStream position to the current element of the zip file (it moves in the recursion targeting the contents of a
   *        ZIP file)
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
    throw new MalformedURLException(format("Can't find the %s file in %s", expectedFile, artifactZip.getName()));
  }

  /**
   * As this class is meant to help a {@link ClassLoader}, it will look for resources within zips, the "classes" is a particular
   * scenario where it will collapse with the subsequent element of the split file, as /classes in a mule module is never
   * compressed, different scenario for /lib folder, where every element there is a compressed jar.
   *
   * @param files to split by {@link #SEPARATOR}
   * @return the elements to work with when opening the stream
   */
  private List<String> getFiles(final String files) {
    if (files.isEmpty()) {
      throw new IllegalArgumentException(format("There's no file to process after the first '%s' (full URL received '%s')",
                                                SEPARATOR, url.toString()));
    }
    String[] resources = files.split(SEPARATOR);
    if (resources.length == 2 && !isCompressed(resources[0])) {
      // this scenario handles the /classes!/org/foo/echo/MyClass.class
      return asList(resources[0].concat("/").concat(resources[1]));
    }
    return Arrays.stream(resources)
        .map(ParseUtil::decode).collect(Collectors.toList());
  }

  private boolean isCompressed(String resource) {
    return endsWithIgnoreCase(resource, ".zip") || endsWithIgnoreCase(resource, ".jar");
  }
}


/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.xml.bind.DatatypeConverter;

import com.sun.management.HotSpotDiagnosticMXBean;

public class TroubleshootingUtils {

  private TroubleshootingUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static final String PLUGINS_COPIED_FOR_TROUBLESHOOTING_FOLDER = "pluginsCopiedForTroubleshooting";
  public static final String HEAP_DUMP_FILE_NAME = "heapDump.hprof";
  public static final String HEAP_DUMP_ONLY_LIVE_FILE_NAME = "heapDumpOnlyLive.hprof";

  public static Path getJenkinsWorkspacePath(Path pluginJsonUrl) {
    // The workspace name change from job to job with the form <folder>_<job name>_<branch>. Ex 'Mule-runtime_mule-ee_mule-4.x'
    // So we use the ".m2" folder which is in the root of the workspace in all jobs and also works running locally.
    return getPathOfFileContaining(pluginJsonUrl, ".m2").getParent();
  }

  public static Path getJenkinsWorkspacePath(URL pluginJsonUrl) {
    return getJenkinsWorkspacePath(Paths.get(getPathStringFromJarURL(pluginJsonUrl))).getParent();
  }

  public static Path getJarPathFromPluginJson(Path pluginJsonUrl) {
    return getPathOfFileContaining(pluginJsonUrl, "mule-plugin.jar");
  }

  public static Path getJarPathFromPluginJson(URL pluginJsonUrl) {
    return getJarPathFromPluginJson(Paths.get(getPathStringFromJarURL(pluginJsonUrl)));
  }

  public static Path getPathOfFileContaining(URL pluginJsonUrl, String substring) {
    Path pluginJsonPath = Paths.get(getPathStringFromJarURL(pluginJsonUrl));

    return getPathOfFileContaining(pluginJsonPath, substring);
  }

  public static String getPathStringFromJarURL(URL pluginJsonUrl) {
    String pluginJsonString = "";

    if (pluginJsonUrl.toString().contains("jar:")) {
      JarURLConnection connection = null;
      try {
        connection = (JarURLConnection) pluginJsonUrl.openConnection();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      pluginJsonString = connection.getJarFileURL().getFile();
    } else {
      pluginJsonString = pluginJsonUrl.getPath();
    }

    return pluginJsonString;
  }

  public static Path getPathOfFileContaining(Path pluginJsonUrl, String substring) {
    Path auxPath = pluginJsonUrl;
    while (auxPath != null && !auxPath.getFileName().toString().contains(substring)) {
      auxPath = auxPath.getParent();
    }

    if (auxPath == null) {
      throw new RuntimeException(String.format("Could not find the substring '%s' in path '%s'", substring,
                                               pluginJsonUrl.toAbsolutePath()));
    }

    return auxPath;
  }

  public static String getMD5FromFile(Path filePath) {
    String md5Checksum = "";

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(Files.readAllBytes(filePath));
      byte[] digest = md.digest();
      md5Checksum = DatatypeConverter
          .printHexBinary(digest).toLowerCase();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("There was a problem getting MD5 Algorithm from the class MessageDigest", e);
    }

    return md5Checksum;
  }

  public static String getMD5FromFile(URL filePath) {
    return getMD5FromFile(getJarPathFromPluginJson(filePath));
  }

  public static void copyPluginToAuxJenkinsFolderForTroubleshooting(Path pluginJsonUrl) throws IOException {
    Path extensionJarPath = getJarPathFromPluginJson(pluginJsonUrl);
    Path jenkinsTroubleshootingFolderPath = getJenkinsTroubleshootingFolderPath(pluginJsonUrl);

    Path extensionJarTargetPath =
        Paths.get(jenkinsTroubleshootingFolderPath.toString(), extensionJarPath.getFileName().toString());
    Files.copy(extensionJarPath, extensionJarTargetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
  }

  public static void copyPluginToAuxJenkinsFolderForTroubleshooting(URL pluginJsonUrl) throws IOException {
    copyPluginToAuxJenkinsFolderForTroubleshooting(Paths.get(getPathStringFromJarURL(pluginJsonUrl)));
  }

  public static Path getJenkinsTroubleshootingFolderPath(Path pluginJsonUrl) throws IOException {
    Path jenkinsTroubleshootingFolderPath =
        Paths.get(getJenkinsWorkspacePath(pluginJsonUrl).toString(), PLUGINS_COPIED_FOR_TROUBLESHOOTING_FOLDER);

    if (!jenkinsTroubleshootingFolderPath.toFile().exists()) {
      Files.createDirectories(jenkinsTroubleshootingFolderPath);
    }

    return jenkinsTroubleshootingFolderPath;
  }

  public static void generateHeapDumpInAuxJenkinsFolder(Path pluginJsonUrl) throws IOException {
    Path jenkinsTroubleshootingFolderPath = getJenkinsTroubleshootingFolderPath(pluginJsonUrl);
    dumpHeap(Paths.get(jenkinsTroubleshootingFolderPath.toString(), HEAP_DUMP_FILE_NAME).toString(), false);
    dumpHeap(Paths.get(jenkinsTroubleshootingFolderPath.toString(), HEAP_DUMP_ONLY_LIVE_FILE_NAME).toString(), true);
  }

  public static void generateHeapDumpInAuxJenkinsFolder(URL pluginJsonUrl) throws IOException {
    generateHeapDumpInAuxJenkinsFolder(Paths.get(getPathStringFromJarURL(pluginJsonUrl)));
  }

  public static void dumpHeap(String filePath, boolean live) throws IOException {
    if (Files.exists(Paths.get(filePath))) {
      return;
    }

    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    HotSpotDiagnosticMXBean mxBean = ManagementFactory
        .newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic",
                                HotSpotDiagnosticMXBean.class);
    mxBean.dumpHeap(filePath, live);
  }

  public static String getLastModifiedDateFromUrl(URL filePathURL) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long lastModifiedDate = 0L;

    try {
      lastModifiedDate = new File(filePathURL.toURI()).lastModified();
    } catch (URISyntaxException e) {
      //do nothing
    }

    return sdf.format(lastModifiedDate);
  }

  public static List<String> listEntriesInJar(File jarPath) throws IOException {
    List<String> entriesInJarList = new ArrayList<>();

    entriesInJarList.add("name: '" + jarPath.getName() + "' - last modified time: '"
        + getLastModifiedDateFromUrl(jarPath.toURI().toURL()) + "'");

    try (JarFile jarFile = new JarFile(jarPath)) {
      Enumeration<JarEntry> jarEntries = jarFile.entries();
      while (jarEntries.hasMoreElements()) {
        JarEntry entry = jarEntries.nextElement();
        String entryName = entry.getName();
        FileTime entryLastModifiedTime = entry.getLastModifiedTime();

        entriesInJarList.add("name: '" + entryName + "' - last modified time: '" + entryLastModifiedTime + "'");
      }
    }
    return entriesInJarList;
  }
}

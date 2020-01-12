/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.utils;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.util.Reference;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.management.MBeanServer;
import javax.xml.bind.DatatypeConverter;

import com.sun.management.HotSpotDiagnosticMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TroubleshootingUtils {

  private static final Optional<String> JENKINS_WORKSPACE_PATH = ofNullable(System.getProperty("jenkins.workspace.path"));
  private static final String HEISENBERG_ARTIFACT_LOCATION = "org/mule/tests/mule-heisenberg-extension/4.3.0-SNAPSHOT";

  private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
  private static final String OS_NAME_FAMILY = getOsNameFamily();
  private static final Map<String, String> GET_OS_PROCESSES_COMMANDS = initializeGetOSProcessesCommands();

  public static final String PLUGINS_COPIED_FOR_TROUBLESHOOTING_FOLDER = "pluginsCopiedForTroubleshooting";
  public static final String HEAP_DUMP_FILE_NAME = "heapDump.hprof";
  public static final String HEAP_DUMP_ONLY_LIVE_FILE_NAME = "heapDumpOnlyLive.hprof";
  public static final String OS_PROCESSES_LOG_NAME = "osProcessesFile.log";

  private static final Logger LOGGER = LoggerFactory.getLogger(TroubleshootingUtils.class);

  private TroubleshootingUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static Path getJenkinsWorkspacePath(Path pluginJsonUrl) {
    // The workspace name change from job to job with the form <folder>_<job name>_<branch>. Ex 'Mule-runtime_mule-ee_mule-4.x'
    // So we use the ".m2" folder which is in the root of the workspace in all jobs and also works running locally.
    return getPathOfFileContaining(pluginJsonUrl, ".m2").getParent();
  }

  public static Optional<Path> getJenkinsWorkspacePath() {
    // The workspace name change from job to job with the form <folder>_<job name>_<branch>. Ex 'Mule-"runtime_mule-ee_mule-4.x'
    // So we use the ".m2" folder which is in the root of the workspace in all jobs and also works running locally.
    return JENKINS_WORKSPACE_PATH.map(p -> Paths.get(p));
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

    if (pluginJsonUrl != null && auxPath == null) {
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

  public static void copyRepoContentsToAuxJenkinsFolder() throws IOException {
    Reference<IOException> exceptionReference = new Reference<>();
    getJenkinsWorkspacePath().ifPresent(
                                        jenkinsWorkspacePath -> {
                                          Path heisenbergSnapshotRoot = jenkinsWorkspacePath.resolve(".m2").resolve("repository")
                                              .resolve(HEISENBERG_ARTIFACT_LOCATION);

                                          if (!heisenbergSnapshotRoot.toFile().exists()) {
                                            return;
                                          }

                                          try {
                                            Path jenkinsTroubleshootingFolderPath =
                                                getJenkinsTroubleshootingFolderPath(heisenbergSnapshotRoot);

                                            Path repoContents = jenkinsTroubleshootingFolderPath.resolve("repoContents");

                                            if (repoContents.toFile().exists()) {
                                              return;
                                            }

                                            repoContents.toFile().mkdirs();

                                            for (File f : heisenbergSnapshotRoot.toFile().listFiles()) {
                                              Files.copy(f.toPath(), repoContents.resolve(f.getName()));
                                            }
                                          } catch (IOException e) {
                                            exceptionReference.set(e);
                                          }
                                        });

    if (exceptionReference.get() != null) {
      throw exceptionReference.get();
    }
  }

  public static void copyFileToAuxJenkinsFolder(Path fileToCopy) throws IOException {
    Path jenkinsTroubleshootingFolderPath = getJenkinsTroubleshootingFolderPath(fileToCopy);

    Path extensionJarTargetPath =
        Paths.get(jenkinsTroubleshootingFolderPath.toString(), fileToCopy.getFileName().toString());
    Files.copy(fileToCopy, extensionJarTargetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
  }

  public static void copyPluginToAuxJenkinsFolderForTroubleshooting(Path pluginJsonUrl) throws IOException {
    copyFileToAuxJenkinsFolder(getJarPathFromPluginJson(pluginJsonUrl));
  }

  public static void copyPluginToAuxJenkinsFolderForTroubleshooting(URL pluginJsonUrl) throws IOException {
    copyPluginToAuxJenkinsFolderForTroubleshooting(Paths.get(getPathStringFromJarURL(pluginJsonUrl)));
  }

  public static void copyOsRunningProcessesToAuxJenkinsFolder(URL pluginJsonUrl) throws IOException {
    Path jenkinsTroubleshootingFolderPath =
        getJenkinsTroubleshootingFolderPath(Paths.get(getPathStringFromJarURL(pluginJsonUrl)));
    Path osProcessesTargetPath =
        Paths.get(jenkinsTroubleshootingFolderPath.toString(), OS_PROCESSES_LOG_NAME);

    if (osProcessesTargetPath.toFile().exists()) {
      return;
    }

    List<String> runningProcesses = getRunningProcesses();

    Files.write(osProcessesTargetPath, runningProcesses);
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
    if (Paths.get(filePath).toFile().exists()) {
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

  public static List<String> getRunningProcesses() {

    List<String> processesList = new ArrayList<>();

    if (!GET_OS_PROCESSES_COMMANDS.containsKey(OS_NAME_FAMILY)) {
      return processesList;
    }

    String getOsProcessesCommand = GET_OS_PROCESSES_COMMANDS.get(OS_NAME_FAMILY);
    processesList.add(String.format("OS: '%s' - Command: '%s'", OS_NAME, getOsProcessesCommand));

    try {
      Process runningProcessesResult = Runtime.getRuntime().exec(GET_OS_PROCESSES_COMMANDS.get(OS_NAME_FAMILY));
      try (BufferedReader runningProcessesResultReader =
          new BufferedReader(new InputStreamReader(runningProcessesResult.getInputStream()))) {
        String runningProcessLine;

        while ((runningProcessLine = runningProcessesResultReader.readLine()) != null) {
          processesList.add(runningProcessLine);
        }
      }
    } catch (IOException e) {
      LOGGER.trace(e.getMessage());
    }

    return processesList;
  }

  public static String getOsNameFamily() {
    String osFamily;

    if (isUnix()) {
      osFamily = "unix";
    } else if (isMac()) {
      osFamily = "mac";
    } else if (isWindows()) {
      osFamily = "windows";
    } else {
      osFamily = "other";
    }

    return osFamily;
  }

  private static boolean isUnix() {
    return OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
  }

  private static boolean isMac() {
    return OS_NAME.contains("mac");
  }

  private static boolean isWindows() {
    return OS_NAME.startsWith("win");
  }

  private static Map<String, String> initializeGetOSProcessesCommands() {
    Map<String, String> getProcessesCommand = new HashMap<>();

    getProcessesCommand.put("unix", "ps -aux");
    getProcessesCommand.put("mac", "ps -e");

    return getProcessesCommand;
  }

}

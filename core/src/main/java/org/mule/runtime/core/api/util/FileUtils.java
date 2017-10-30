/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.System.getProperty;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.IOUtils.copy;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>FileUtils</code> contains useful methods for dealing with files & directories.
 */
// @ThreadSafe
public class FileUtils {

  private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
  private static final String TEMP_DIR_SYSTEM_PROPERTY = "java.io.tmpdir";
  private static final File TEMP_DIR = new File(getProperty(TEMP_DIR_SYSTEM_PROPERTY));
  private static final AtomicLong TEMP_FILE_INDEX = new AtomicLong(-1);

  public static String DEFAULT_ENCODING = "UTF-8";

  static {
    if (!TEMP_DIR.exists()) {
      throw new MuleRuntimeException(createStaticMessage("Temp directory '" + TEMP_DIR.getAbsolutePath() + "' does not exist. "
          + "Please check the value of the '" + TEMP_DIR_SYSTEM_PROPERTY
          + "' system property."));
    }
  }

  public static synchronized void copyStreamToFile(InputStream input, File destination) throws IOException {
    if (destination.exists() && !destination.canWrite()) {
      throw new IOException("Destination file does not exist or is not writeable");
    }

    try {
      FileOutputStream output = new FileOutputStream(destination);
      try {
        copy(input, output);
      } finally {
        IOUtils.closeQuietly(output);
      }
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Cleans a directory without deleting it.
   *
   * @param directory directory to clean
   * @throws IOException in case cleaning is unsuccessful
   */
  public static void cleanDirectory(File directory) throws IOException {
    org.apache.commons.io.FileUtils.cleanDirectory(directory);
  }

  // TODO Document me!
  public static File createFile(String filename) throws IOException {
    File file = FileUtils.newFile(filename);
    if (!file.canWrite()) {
      String dirName = file.getPath();
      int i = dirName.lastIndexOf(File.separator);
      if (i > -1) {
        dirName = dirName.substring(0, i);
        File dir = FileUtils.newFile(dirName);
        dir.mkdirs();
      }
      file.createNewFile();
    }
    return file;
  }

  // TODO Document me!
  public static String prepareWinFilename(String filename) {
    filename = filename.replaceAll("<", "(");
    filename = filename.replaceAll(">", ")");
    filename = filename.replaceAll("[/\\*?|:;\\]\\[\"]", "-");
    return filename;
  }

  // TODO Document me!
  public static File openDirectory(String directory) throws IOException {
    File dir = FileUtils.newFile(directory);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    if (!dir.isDirectory() || !dir.canRead()) {
      throw new IOException("Path: " + directory + " exists but isn't a directory");
    }
    return dir;
  }

  /**
   * Reads the incoming String into a file at at the given destination.
   *
   * @param filename name and path of the file to create
   * @param data the contents of the file
   * @return the new file.
   * @throws IOException If the creating or writing to the file stream fails
   */
  public static File stringToFile(String filename, String data) throws IOException {
    return stringToFile(filename, data, false);
  }

  // TODO Document me!
  public static synchronized File stringToFile(String filename, String data, boolean append) throws IOException {
    return stringToFile(filename, data, append, false);
  }

  // TODO Document me!
  public static synchronized File stringToFile(String filename, String data, boolean append, boolean newLine) throws IOException {
    File f = createFile(filename);
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new FileWriter(f, append));
      writer.write(data);
      if (newLine) {
        writer.newLine();
      }
    } finally {
      if (writer != null) {
        writer.close();
      }
    }
    return f;
  }

  // TODO Document me!
  public static String getResourcePath(String resourceName, Class callingClass) throws IOException {
    return getResourcePath(resourceName, callingClass, DEFAULT_ENCODING);
  }

  // TODO Document me!
  public static String getResourcePath(String resourceName, Class callingClass, String encoding) throws IOException {
    if (resourceName == null) {
      // no name
      return null;
    }

    URL url = IOUtils.getResourceAsUrl(resourceName, callingClass);
    if (url == null) {
      // not found
      return null;
    }
    return normalizeFilePath(url, encoding);
  }

  /**
   * Remove from uri to file prefix file:/ Add if need file separator to begin
   *
   * @param url file uri to resource
   * @param encoding - Java encoding names
   * @return normalized file path
   * @throws UnsupportedEncodingException if encoding is unknown
   */
  public static String normalizeFilePath(URL url, String encoding) throws UnsupportedEncodingException {
    String resource = URLDecoder.decode(url.toExternalForm(), encoding);
    if (resource != null) {
      if (resource.startsWith("file:/")) {
        resource = resource.substring(6);

        if (!resource.startsWith(File.separator)) {
          resource = File.separator + resource;
        }
      }
    }
    return resource;
  }


  /**
   * Delete a file tree recursively.
   * 
   * @param dir dir to wipe out
   * @return false when the first unsuccessful attempt encountered
   */
  public static boolean deleteTree(File dir) {
    return deleteTree(dir, null);
  }

  /**
   * Delete a file tree recursively. This method additionally tries to be gentle with specified top-level dirs. E.g. this is the
   * case when a transaction manager asynchronously handles the recovery log, and the test wipes out everything, leaving the
   * transaction manager puzzled.
   * 
   * @param dir dir to wipe out
   * @param topLevelDirsToIgnore which top-level directories to ignore, if null or empty then ignored
   * @return false when the first unsuccessful attempt encountered
   */
  public static boolean deleteTree(File dir, final String[] topLevelDirsToIgnore) {
    if (dir == null || !dir.exists()) {
      return true;
    }
    File[] files = dir.listFiles();
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        OUTER: if (files[i].isDirectory()) {
          if (topLevelDirsToIgnore != null) {
            for (int j = 0; j < topLevelDirsToIgnore.length; j++) {
              String ignored = topLevelDirsToIgnore[j];
              if (ignored.equals(getBaseName(files[i].getName()))) {
                break OUTER;
              }
            }
          }
          if (!deleteTree(files[i])) {
            return false;
          }
        } else {
          if (!files[i].delete()) {
            return false;
          }
        }
      }
    }
    return dir.delete();
  }

  /**
   * Unzip the specified archive to the given directory
   */
  public static void unzip(File archive, File directory) throws IOException {
    ZipFile zip = null;

    if (directory.exists()) {
      if (!directory.isDirectory()) {
        throw new IOException("Directory is not a directory: " + directory);
      }
    } else {
      if (!directory.mkdirs()) {
        throw new IOException("Could not create directory: " + directory);
      }
    }
    try {
      zip = new ZipFile(archive);
      for (Enumeration entries = zip.entries(); entries.hasMoreElements();) {
        ZipEntry entry = (ZipEntry) entries.nextElement();
        File f = FileUtils.newFile(directory, entry.getName());
        if (entry.isDirectory()) {
          if (!f.exists() && !f.mkdirs()) {
            throw new IOException("Could not create directory: " + f);
          }
        } else {
          File file = new File(directory, entry.getName());
          if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Unable to create folders for zip entry: " + entry.getName());
          }

          InputStream is = zip.getInputStream(entry);
          OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
          copy(is, os);
          IOUtils.closeQuietly(is);
          IOUtils.closeQuietly(os);
        }
      }
    } finally {
      if (zip != null) {
        zip.close();
      }
    }
  }

  /**
   * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557"> 4117557</a>. More
   * in-context information at <a href="http://mule.mulesoft.org/jira/browse/MULE-1112">MULE-1112</a>
   * <p/>
   * Factory methods correspond to constructors of the <code>java.io.File class</code>. No physical file created in this method.
   *
   * @see File
   */
  public static File newFile(String pathName) {
    try {
      return new File(pathName).getCanonicalFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Unable to create a canonical file for " + pathName),
                                     e);
    }
  }

  /**
   * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557"> 4117557</a>. More
   * in-context information at <a href="http://mule.mulesoft.org/jira/browse/MULE-1112">MULE-1112</a>
   * <p/>
   * Factory methods correspond to constructors of the <code>java.io.File class</code>. No physical file created in this method.
   *
   * @see File
   */
  public static File newFile(URI uri) {
    try {
      return new File(uri).getCanonicalFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(createStaticMessage("Unable to create a canonical file for " + uri), e);
    }
  }

  /**
   * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557"> 4117557</a>. More
   * in-context information at <a href="http://mule.mulesoft.org/jira/browse/MULE-1112">MULE-1112</a>
   * <p/>
   * Factory methods correspond to constructors of the <code>java.io.File class</code>. No physical file created in this method.
   *
   * @see File
   */
  public static File newFile(File parent, String child) {
    try {
      return new File(parent, child).getCanonicalFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage("Unable to create a canonical file for parent: " + parent
                                         + " and child: " + child),
                                     e);
    }
  }

  /**
   * Workaround for JDK bug <a href="http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4117557"> 4117557</a>. More
   * in-context information at <a href="http://mule.mulesoft.org/jira/browse/MULE-1112">MULE-1112</a>
   * <p/>
   * Factory methods correspond to constructors of the <code>java.io.File class</code>. No physical file created in this method.
   *
   * @see File
   */
  public static File newFile(String parent, String child) {
    try {
      return new File(parent, child).getCanonicalFile();
    } catch (IOException e) {
      throw new MuleRuntimeException(
                                     createStaticMessage("Unable to create a canonical file for parent: " + parent
                                         + " and child: " + child),
                                     e);
    }
  }

  /**
   * Extract the specified resource to the given directory for remain all directory struct
   *
   * @param resourceName - full resource name
   * @param callingClass - classloader for this class is used
   * @param outputDir - extract to this directory
   * @param keepParentDirectory true - full structure of directories is kept; false - file - removed all directories, directory -
   *        started from resource point
   * @throws IOException if any errors
   */
  public static void extractResources(String resourceName, Class callingClass, File outputDir, boolean keepParentDirectory)
      throws IOException {
    URL url = callingClass.getClassLoader().getResource(resourceName);
    URLConnection connection = url.openConnection();
    if (connection instanceof JarURLConnection) {
      extractJarResources((JarURLConnection) connection, outputDir, keepParentDirectory);
    } else {
      extractFileResources(normalizeFilePath(url, DEFAULT_ENCODING), outputDir, resourceName, keepParentDirectory);
    }
  }

  /**
   * Extract resources contain in file
   *
   * @param path - path to file
   * @param outputDir Directory for unpack recources
   * @param resourceName
   * @param keepParentDirectory true - full structure of directories is kept; false - file - removed all directories, directory -
   *        started from resource point
   * @throws IOException if any error
   */
  private static void extractFileResources(String path, File outputDir, String resourceName, boolean keepParentDirectory)
      throws IOException {
    File file = FileUtils.newFile(path);
    if (!file.exists()) {
      throw new IOException("The resource by path " + path + " ");
    }
    if (file.isDirectory()) {
      if (keepParentDirectory) {
        outputDir = FileUtils.newFile(outputDir.getPath() + File.separator + resourceName);
        if (!outputDir.exists()) {
          outputDir.mkdirs();
        }
      } else {
        outputDir = FileUtils.newFile(outputDir.getPath());
      }
      org.apache.commons.io.FileUtils.copyDirectory(file, outputDir);
    } else {

      if (keepParentDirectory) {
        outputDir = FileUtils.newFile(outputDir.getPath() + File.separator + resourceName);
      } else {
        outputDir = FileUtils.newFile(outputDir.getPath() + File.separator + file.getName());
      }
      org.apache.commons.io.FileUtils.copyFile(file, outputDir);
    }
  }

  /**
   * Extract recources contain if jar (have to in classpath)
   *
   * @param connection JarURLConnection to jar library
   * @param outputDir Directory for unpack recources
   * @param keepParentDirectory true - full structure of directories is kept; false - file - removed all directories, directory -
   *        started from resource point
   * @throws IOException if any error
   */
  private static void extractJarResources(JarURLConnection connection, File outputDir, boolean keepParentDirectory)
      throws IOException {
    JarFile jarFile = connection.getJarFile();
    JarEntry jarResource = connection.getJarEntry();
    Enumeration entries = jarFile.entries();
    InputStream inputStream = null;
    OutputStream outputStream = null;
    int jarResourceNameLenght = jarResource.getName().length();
    for (; entries.hasMoreElements();) {
      JarEntry entry = (JarEntry) entries.nextElement();
      if (entry.getName().startsWith(jarResource.getName())) {

        String path = outputDir.getPath() + File.separator + entry.getName();

        // remove directory struct for file and first dir for directory
        if (!keepParentDirectory) {
          if (entry.isDirectory()) {
            if (entry.getName().equals(jarResource.getName())) {
              continue;
            }
            path =
                outputDir.getPath() + File.separator + entry.getName().substring(jarResourceNameLenght, entry.getName().length());
          } else {
            if (entry.getName().length() > jarResourceNameLenght) {
              path = outputDir.getPath() + File.separator
                  + entry.getName().substring(jarResourceNameLenght, entry.getName().length());
            } else {
              path = outputDir.getPath() + File.separator
                  + entry.getName().substring(entry.getName().lastIndexOf("/"), entry.getName().length());
            }
          }
        }

        File file = FileUtils.newFile(path);
        if (!file.getParentFile().exists()) {
          if (!file.getParentFile().mkdirs()) {
            throw new IOException("Could not create directory: " + file.getParentFile());
          }
        }
        if (entry.isDirectory()) {
          if (!file.exists() && !file.mkdirs()) {
            throw new IOException("Could not create directory: " + file);
          }

        } else {
          try {
            inputStream = jarFile.getInputStream(entry);
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            copy(inputStream, outputStream);
          } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
          }
        }

      }
    }
  }

  public static boolean renameFileHard(String srcFilePath, String destFilePath) {
    if (StringUtils.isNotBlank(srcFilePath) && StringUtils.isNotBlank(destFilePath)) {
      return renameFileHard(new File(srcFilePath), new File(destFilePath));
    } else {
      return false;
    }
  }

  public static boolean renameFileHard(File srcFile, File destFile) {
    boolean isRenamed = false;
    if (srcFile != null && destFile != null) {
      logger.debug("Moving file " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
      if (!destFile.exists()) {
        try {
          if (srcFile.isFile()) {
            logger.debug("Trying to rename file");
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
              in = new FileInputStream(srcFile);
              out = new FileOutputStream(destFile);
              out.getChannel().transferFrom(in.getChannel(), 0, srcFile.length());
              isRenamed = true;
            } catch (Exception e) {
              logger.debug("Error renaming file", e);
            } finally {
              if (in != null) {
                try {
                  in.close();
                } catch (Exception inNotClosed) {
                  logger.debug("Error closing input file", inNotClosed);
                }
              }
              if (out != null) {
                try {
                  out.close();
                } catch (Exception outNotClosed) {
                  logger.debug("Error closing output file", outNotClosed);
                }
              }
            }
            logger.debug("File renamed: " + isRenamed);
            if (isRenamed) {
              srcFile.delete();
            } else {
              destFile.delete();
            }
          } else {
            logger.debug(srcFile.getAbsolutePath() + " is not a valid file.");
          }
        } catch (Exception e) {
          logger.debug("Error renaming file from " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
        }
      } else {
        logger.debug("Error renaming file " + srcFile.getAbsolutePath() + ". Destination file " + destFile.getAbsolutePath()
            + " already exists.");
      }
    }
    return isRenamed;
  }

  public static boolean renameFile(String srcFilePath, String destFilePath) {
    if (StringUtils.isNotBlank(srcFilePath) && StringUtils.isNotBlank(destFilePath)) {
      return renameFile(new File(srcFilePath), new File(destFilePath));
    } else {
      return false;
    }
  }

  public static boolean renameFile(File srcFile, File destFile) {
    boolean isRenamed = false;
    if (srcFile != null && destFile != null) {
      logger.debug("Moving file " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath());
      if (!destFile.exists()) {
        try {
          if (srcFile.isFile()) {
            logger.debug("Trying to rename file");
            isRenamed = srcFile.renameTo(destFile);
            if (!isRenamed && srcFile.exists()) {
              logger.debug("Trying hard copy, assuming partition crossing ...");
              isRenamed = renameFileHard(srcFile, destFile);
            }
            logger.debug("File renamed: " + isRenamed);
          } else {
            logger.debug(srcFile.getAbsolutePath() + " is not a valid file");
          }
        } catch (Exception e) {
          logger.debug("Error moving file from " + srcFile.getAbsolutePath() + " to " + destFile.getAbsolutePath(), e);
        }
      } else {
        logger.debug("Error renaming file " + srcFile.getAbsolutePath() + ". Destination file " + destFile.getAbsolutePath()
            + " already exists.");
      }
    } else {
      logger.debug("Error renaming file. Source or destination file is null.");
    }

    return isRenamed;
  }

  /**
   * Try to move a file by renaming with backup attempt by copying/deleting via NIO. Creates intermidiate directories as required.
   */
  public static boolean moveFileWithCopyFallback(File sourceFile, File destinationFile) {
    // try fast file-system-level move/rename first
    boolean success = sourceFile.renameTo(destinationFile);

    if (!success) {
      // try again using NIO copy
      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
        fis = new FileInputStream(sourceFile);
        if (!destinationFile.exists()) {
          FileUtils.createFile(destinationFile.getPath());
        }
        fos = new FileOutputStream(destinationFile);
        FileChannel srcChannel = fis.getChannel();
        FileChannel dstChannel = fos.getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
        success = sourceFile.delete();
      } catch (IOException ioex) {
        // grr!
        success = false;
      } finally {
        IOUtils.closeQuietly(fis);
        IOUtils.closeQuietly(fos);
      }
    }

    return success;
  }


  // Override the following methods to use a new version of doCopyFile(File
  // srcFile, File destFile, boolean preserveFileDate) that uses nio to copy file

  /**
   * Copies a file to a new location.
   * <p>
   * This method copies the contents of the specified source file to the specified destination file. The directory holding the
   * destination file is created if it does not exist. If the destination file exists, then this method will overwrite it.
   * 
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destFile the new file, must not be <code>null</code>
   * @param preserveFileDate true if the file date of the copy should be the same as the original
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see org.apache.commons.io.FileUtils#copyFileToDirectory(File, File, boolean)
   */
  public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
    if (srcFile == null) {
      throw new NullPointerException("Source must not be null");
    }
    if (destFile == null) {
      throw new NullPointerException("Destination must not be null");
    }
    if (srcFile.exists() == false) {
      throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
    }
    if (srcFile.isDirectory()) {
      throw new IOException("Source '" + srcFile + "' exists but is a directory");
    }
    if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
      throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
    }
    if (destFile.getParentFile() != null && destFile.getParentFile().exists() == false) {
      if (destFile.getParentFile().mkdirs() == false) {
        throw new IOException("Destination '" + destFile + "' directory cannot be created");
      }
    }
    if (destFile.exists() && destFile.canWrite() == false) {
      throw new IOException("Destination '" + destFile + "' exists but is read-only");
    }
    doCopyFile(srcFile, destFile, preserveFileDate);
  }

  /**
   * Internal copy file method.
   * 
   * @param srcFile the validated source file, must not be <code>null</code>
   * @param destFile the validated destination file, must not be <code>null</code>
   * @param preserveFileDate whether to preserve the file date
   * @throws IOException if an error occurs
   */
  private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
    if (destFile.exists() && destFile.isDirectory()) {
      throw new IOException("Destination '" + destFile + "' exists but is a directory");
    }

    FileChannel input = new FileInputStream(srcFile).getChannel();
    try {
      FileChannel output = new FileOutputStream(destFile).getChannel();
      try {
        output.transferFrom(input, 0, input.size());
      } finally {
        closeQuietly(output);
      }
    } finally {
      closeQuietly(input);
    }

    if (srcFile.length() != destFile.length()) {
      throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
    }
    if (preserveFileDate) {
      destFile.setLastModified(srcFile.lastModified());
    }
  }

  /**
   * Unconditionally close a <code>Channel</code>.
   * <p>
   * Equivalent to {@link Channel#close()}, except any exceptions will be ignored. This is typically used in finally blocks.
   * 
   * @param channel the Channel to close, may be null or already closed
   */
  public static void closeQuietly(Channel channel) {
    try {
      if (channel != null) {
        channel.close();
      }
    } catch (IOException ioe) {
      // ignore
    }
  }

  public static boolean isFile(URL url) {
    return "file".equals(url.getProtocol());
  }

  private static Collection<File> findFiles(File folder, IOFileFilter filter, boolean recursive) {
    return org.apache.commons.io.FileUtils.listFiles(folder, filter,
                                                     recursive ? TrueFileFilter.INSTANCE : FalseFileFilter.INSTANCE);
  }

  public static File findFileByName(File folder, final String filename) {
    Collection<File> files = FileUtils.findFiles(folder, new IOFileFilter() {

      @Override
      public boolean accept(File file) {
        return filename.equals(file.getName());
      }

      @Override
      public boolean accept(File dir, String name) {
        return true;
      }
    }, true);

    return isEmpty(files) ? null : files.iterator().next();
  }

  /**
   * Creates a temporal file for buffering. The file is stored in the system temporal
   * folder.
   *
   * @param prefix the file's prefix
   * @param suffix the file's suffix
   * @return a {@link File}
   * @throws RuntimeException
   */
  public static File createTempFile(String prefix, String suffix) {
    return new File(TEMP_DIR, prefix + TEMP_FILE_INDEX.addAndGet(1) + suffix);
  }
}

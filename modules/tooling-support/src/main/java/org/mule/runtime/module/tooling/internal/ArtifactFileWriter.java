/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeByteArrayToFile;
import static org.mule.runtime.core.api.util.FileUtils.unzip;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Utility class to handle artifact contents and write it to the file system.
 *
 * @since 2.0
 */
public class ArtifactFileWriter {

  private static final String JAR = ".jar";

  private File folder;

  /**
   * Creates an instance of the writer to work in the given folder.
   *
   * @param folder {@link File} folder were artifacts will be unzipped.
   */
  public ArtifactFileWriter(File folder) {
    this.folder = folder;
  }

  /**
   * Writes the jar file content to a folder with the given artifactName.
   *
   * @param artifactName the name of the folder to where jar content will be exploded.
   * @param artifactContentLocation the jar file for the artifact.
   * @return the directory where file has been written.
   */
  public File explodeJarContent(String artifactName, File artifactContentLocation) {
    if (!artifactContentLocation.getAbsolutePath().toString().toLowerCase().endsWith(JAR)) {
      throw new IllegalArgumentException("Artifact content should be a jar, actual: "
          + artifactContentLocation.getAbsolutePath());
    }
    File directory = new File(folder, artifactName);
    directory.deleteOnExit();
    File jarFile = artifactContentLocation;
    try {
      unzip(jarFile, directory);
      return directory;
    } catch (IOException e) {
      throw new UncheckedIOException("Error while doing unzip of artifact '" + artifactContentLocation.getAbsolutePath() + "'",
                                     e);
    }
  }

  /**
   * Writes application content from artifactContentLocation to a new artifact folder under {@link #folder} with the artifactName.
   *
   * @param artifactName {@link String} artifact name to create the folder were artifact content will be copied.
   * @param artifactContentLocation {@link File} location of the artifact to be copied.
   * @return {@link File} folder created for the artifact
   */
  public File writeContent(String artifactName, File artifactContentLocation) {
    File artifactFolder = new File(folder, artifactName);
    boolean appFolderCreated = artifactFolder.mkdir();
    if (!appFolderCreated) {
      throw new UncheckedIOException("Error while creating a folder to copy artifact content: " + artifactContentLocation,
                                     new IOException("Cannot create folder: " + artifactFolder));
    }
    artifactFolder.deleteOnExit();

    try {
      walkFileTree(artifactContentLocation.toPath(),
                   new CopyDirVisitor(artifactContentLocation.toPath(), artifactFolder.toPath()));
    } catch (IOException e) {
      throw new UncheckedIOException("Error while copying content of application from: " + artifactContentLocation, e);
    }
    return artifactFolder;
  }

  /**
   * Unzips the content of the artifact and writes it to a folder inside the {@link #folder} with the given artifact name.
   *
   * @param artifactName {@link String} name for the artifact
   * @param zipContent {@code byte[]} with the artifact contented zipped
   * @return {@link File} folder where the artifact was unzipped
   */
  public File writeContent(String artifactName, byte[] zipContent) {
    File artifactFolder = new File(folder, artifactName);
    boolean artifactFolderCreated = artifactFolder.mkdir();
    if (!artifactFolderCreated) {
      throw new UncheckedIOException("Error while creating a folder for artifact: " + artifactName,
                                     new IOException("Cannot create folder: " + artifactFolder));
    }
    artifactFolder.deleteOnExit();

    write(artifactName, zipContent, artifactFolder);
    return artifactFolder;
  }

  private void write(String artifactName, byte[] zipContent, File directory) {
    File jarFile = null;
    try {
      jarFile = new File(folder, artifactName + JAR);
      writeByteArrayToFile(jarFile, zipContent);
      unzip(jarFile, directory);
    } catch (IOException e) {
      throw new UncheckedIOException("Error while doing unzip of artifact '" + artifactName + "'", e);
    } finally {
      if (jarFile != null) {
        deleteQuietly(jarFile);
      }
    }
  }

  private class CopyDirVisitor extends SimpleFileVisitor<Path> {

    private Path fromPath;
    private Path toPath;
    private StandardCopyOption copyOption = REPLACE_EXISTING;

    public CopyDirVisitor(Path fromPath, Path toPath) {
      this.fromPath = fromPath;
      this.toPath = toPath;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      Path targetPath = toPath.resolve(fromPath.relativize(dir));
      if (!exists(targetPath)) {
        createDirectory(targetPath);
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      copy(file, toPath.resolve(fromPath.relativize(file)), copyOption);
      return CONTINUE;
    }
  }

}

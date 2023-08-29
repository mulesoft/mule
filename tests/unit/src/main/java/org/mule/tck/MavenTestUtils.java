/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.tck.ZipUtils.compress;

import static java.util.Arrays.asList;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.MavenPomModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MavenTestUtils {

  private static final String POM = "pom";

  public static Collection<File> installArtifact(File artifactFile, File repositoryLocation) throws IOException {
    String artifactExtension = getExtension(artifactFile.getName());
    MavenPomParser parser = discoverProvider().createMavenPomParserClient(artifactFile.toPath());
    if (POM.equals(artifactExtension)) {
      return installArtifact(artifactFile, repositoryLocation, parser);
    } else {
      File packagedArtifact = packageArtifact(artifactFile, parser.getModel());
      return installArtifact(packagedArtifact, repositoryLocation, parser);
    }
  }

  private static File packageArtifact(File explodedArtifactFile, MavenPomModel pomModel) {
    String fileNameInRepo = pomModel.getArtifactId()
        + "-" + pomModel.getVersion()
        + (pomModel.getPackaging() != null && !pomModel.getPackaging().equalsIgnoreCase("jar") ? "-" + pomModel.getPackaging()
            : "")
        + ".jar";
    File compressedFile = new File(explodedArtifactFile, fileNameInRepo);
    compress(compressedFile, listFiles(explodedArtifactFile, null, true).stream()
        .map(f -> new ZipUtils.ZipResource(f.getAbsolutePath(),
                                           getZipEntryName(explodedArtifactFile, f)))
        .toArray(ZipUtils.ZipResource[]::new));
    return compressedFile;
  }

  private static String getZipEntryName(File baseDir, File entryFile) {
    // https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT - 4.4.17.1
    // The path stored MUST NOT contain a drive or device letter, or a leading slash. All slashes MUST be forward slashes '/' as
    // opposed to backwards slashes '\'
    return baseDir.toURI().relativize(entryFile.toURI()).getPath();
  }

  private static Collection<File> installArtifact(File artifactFile, File repositoryLocation, MavenPomParser parser)
      throws IOException {
    MavenPomModel pomModel = parser.getModel();
    List<String> artifactLocationInRepo = new ArrayList<>(asList(pomModel.getGroupId().split("\\.")));
    artifactLocationInRepo.add(pomModel.getArtifactId());
    artifactLocationInRepo.add(pomModel.getVersion());

    Path pathToArtifactLocationInRepo =
        Paths.get(repositoryLocation.getAbsolutePath(), artifactLocationInRepo.toArray(new String[0]));
    File artifactLocationInRepoFile = pathToArtifactLocationInRepo.toFile();

    artifactLocationInRepoFile.mkdirs();

    File repoArtifactFile = new File(pathToArtifactLocationInRepo.toString(), artifactFile.getName());
    copyFile(artifactFile, repoArtifactFile, true);

    // Copy the pom without the classifier.
    String pomFileName = artifactFile.getName().replaceFirst("(.*\\.[0-9]*\\.[0-9]*\\.?[0-9]?).*", "$1") + ".pom";
    File repoPomFile = new File(pathToArtifactLocationInRepo.toString(), pomFileName);
    copyFile(parser.getModel().getPomFile().get(), repoPomFile, true);

    return asList(repoArtifactFile, repoPomFile);
  }

}

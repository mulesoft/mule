/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.tck.ZipUtils.compress;

import static java.util.Arrays.asList;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.tck.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenTestUtils {

  private static final String POM = "pom";

  public static void installArtifact(File artifactFile, File repositoryLocation) throws IOException, XmlPullParserException {
    String artifactExtension = getExtension(artifactFile.getName());
    if (POM.equals(artifactExtension)) {
      installPom(artifactFile, repositoryLocation);
    } else {
      MavenPomParser mavenPomParser = discoverProvider().createMavenPomParserClient(artifactFile.toPath());
      File packagedArtifact = packageArtifact(artifactFile, mavenPomParser.getModel());
      installArtifact(packagedArtifact, repositoryLocation, mavenPomParser);
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
                                           f.getAbsolutePath().substring(explodedArtifactFile.getAbsolutePath().length() + 1)))
        .toArray(ZipUtils.ZipResource[]::new));
    return compressedFile;
  }

  private static void installPom(File pomFile, File repositoryLocation) throws IOException {
    MavenPomParser parser = discoverProvider().createMavenPomParserClient(pomFile.toPath());
    installArtifact(pomFile, repositoryLocation, parser);
  }

  private static void installArtifact(File artifactFile, File repositoryLocation, MavenPomParser parser)
      throws IOException {
    MavenPomModel pomModel = parser.getModel();
    List<String> artifactLocationInRepo = new ArrayList<>(asList(pomModel.getGroupId().split("\\.")));
    artifactLocationInRepo.add(pomModel.getArtifactId());
    artifactLocationInRepo.add(pomModel.getVersion());

    Path pathToArtifactLocationInRepo =
        Paths.get(repositoryLocation.getAbsolutePath(), artifactLocationInRepo.toArray(new String[0]));
    File artifactLocationInRepoFile = pathToArtifactLocationInRepo.toFile();

    artifactLocationInRepoFile.mkdirs();

    copyFile(artifactFile, new File(pathToArtifactLocationInRepo.toString(), artifactFile.getName()), true);

    // Copy the pom without the classifier.
    String pomFileName = artifactFile.getName().replaceFirst("(.*\\.[0-9]*\\.[0-9]*\\.?[0-9]?).*", "$1") + ".pom";
    copyFile(parser.getModel().getPomFile().get(), new File(pathToArtifactLocationInRepo.toString(), pomFileName), true);
  }

}

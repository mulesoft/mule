/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModel;
import static org.mule.tck.ZipUtils.compress;

import org.mule.tck.ZipUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class MavenTestUtils {

  private static final String POM = "pom";

  public static void installArtifact(File artifactFile, File repositoryLocation) throws IOException, XmlPullParserException {
    String artifactExtension = getExtension(artifactFile.getName());
    if (POM.equals(artifactExtension)) {
      installPom(artifactFile, repositoryLocation);
    } else {
      Model pomModel = getPomModel(artifactFile);
      File packagedArtifact = packageArtifact(artifactFile, pomModel);
      installArtifact(packagedArtifact, repositoryLocation, pomModel);
    }
  }

  private static File packageArtifact(File explodedArtifactFile, Model pomModel) {
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

  private static void installPom(File pomFile, File repositoryLocation) throws IOException, XmlPullParserException {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model pomModel;
    try (FileReader pomReader = new FileReader(pomFile)) {
      pomModel = reader.read(pomReader);
    }
    pomModel.setPomFile(pomFile);
    installArtifact(pomFile, repositoryLocation, pomModel);
  }

  private static void installArtifact(File artifactFile, File repositoryLocation, Model pomModel)
      throws IOException {
    List<String> artifactLocationInRepo = new ArrayList<>(asList(pomModel.getGroupId().split("\\.")));
    artifactLocationInRepo.add(pomModel.getArtifactId());
    artifactLocationInRepo.add(pomModel.getVersion());

    Path pathToArtifactLocationInRepo =
        Paths.get(repositoryLocation.getAbsolutePath(), artifactLocationInRepo.toArray(new String[0]));
    File artifactLocationInRepoFile = pathToArtifactLocationInRepo.toFile();

    artifactLocationInRepoFile.mkdirs();

    copyFile(artifactFile, new File(pathToArtifactLocationInRepo.toString(), artifactFile.getName()), true);

    //Copy the pom without the classifier.
    String pomFileName = artifactFile.getName().replaceFirst("(.*\\.[0-9]*\\.[0-9]*\\.?[0-9]?).*", "$1") + ".pom";
    copyFile(pomModel.getPomFile(), new File(pathToArtifactLocationInRepo.toString(), pomFileName), true);
  }

}

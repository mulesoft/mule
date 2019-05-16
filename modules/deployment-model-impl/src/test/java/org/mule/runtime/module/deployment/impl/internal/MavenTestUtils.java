package org.mule.runtime.module.deployment.impl.internal;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModel;

import org.mule.tck.util.CompilerUtils;

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
    if(POM.equals(artifactExtension)) {
      installPom(artifactFile, repositoryLocation);
    }else {
      Model pomModel = getPomModel(artifactFile);
      File packagedArtifact = artifactFile;
      if(artifactFile.isDirectory()) {
        packagedArtifact = packageArtifact(artifactFile);
      }
      installArtifact(packagedArtifact, repositoryLocation, pomModel);
    }
  }

  private static File packageArtifact(File explodedArtifactFile) {
    return new CompilerUtils.JarCompiler().compiling(explodedArtifactFile).compile(explodedArtifactFile.getName());
  }

  private static void installPom(File pomFile, File repositoryLocation) throws IOException, XmlPullParserException {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model pomModel;
    try (FileReader pomReader = new FileReader(pomFile)) {
      pomModel = reader.read(pomReader);
    }
    installArtifact(pomFile, repositoryLocation, pomModel);
  }

  private static void installArtifact(File artifactFile, File repositoryLocation, Model pomModel) throws IOException, XmlPullParserException {
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
    copyFile(artifactFile, new File(pathToArtifactLocationInRepo.toString(), pomFileName), true);
  }

}

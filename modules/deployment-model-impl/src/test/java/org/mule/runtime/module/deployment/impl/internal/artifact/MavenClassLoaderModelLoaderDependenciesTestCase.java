/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.artifact;

import static com.google.common.io.Files.createTempDir;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.util.FileUtils.copyFile;
import static org.mule.runtime.module.deployment.impl.internal.BundleDependencyMatcher.bundleDependency;
import static org.mule.runtime.module.deployment.impl.internal.artifact.MavenClassLoaderModelLoaderConfigurationTestCase.MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION;
import static org.mule.runtime.module.deployment.impl.internal.maven.MavenUtils.getPomModel;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.tck.junit4.rule.SystemProperty;

import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class MavenClassLoaderModelLoaderDependenciesTestCase extends MavenClassLoaderModelLoaderTestCase {

  @ClassRule
  public static SystemProperty repositoryLocation = new SystemProperty(MULE_RUNTIME_CONFIG_MAVEN_REPOSITORY_LOCATION,
                                                                       createTempDir().getAbsolutePath());

  @BeforeClass
  public static void setUp() throws Exception {
    GlobalConfigLoader.reset();

    //Install all dependencies
    File dependenciesFolder =
        new File(MavenClassLoaderModelLoaderDependenciesTestCase.class.getClassLoader().getResource("dependencies").toURI());
    for (File dependencyFile : dependenciesFolder.listFiles()) {
      if (!dependencyFile.isDirectory()) {
        installDependency(dependencyFile);
      }
    }
  }

  @Test
  public void apiTestCase() throws Exception {
    File artifactFile = getApplicationFolder("apps/test-app");
    ClassLoaderModel classLoaderModel = loadClassLoaderModel(artifactFile);
    assertThat(classLoaderModel.getDependencies(), hasItems(
                                                            bundleDependency("raml-api-a"),
                                                            bundleDependency("raml-api-b"),
                                                            bundleDependency("raml-fragment", "1.0.0"),
                                                            bundleDependency("raml-fragment", "2.0.0")));
  }

  private ClassLoaderModel loadClassLoaderModel(File artifactFile) throws InvalidDescriptorLoaderException {
    Model model = getPomModel(artifactFile);
    Map<String, Object> attributes =
        ImmutableMap.of(org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.class.getName(),
                        new org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.Builder()
                            .setGroupId(model.getGroupId())
                            .setArtifactId(model.getArtifactId())
                            .setVersion(model.getVersion())
                            .setType("jar")
                            .setClassifier("mule-application")
                            .build());
    return mavenClassLoaderModelLoader.load(artifactFile, attributes, APP);
  }


  private static void installDependency(File dependencyFile) throws IOException, XmlPullParserException {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model pomModel;
    try (FileReader pomReader = new FileReader(dependencyFile)) {
      pomModel = reader.read(pomReader);
    }

    List<String> dependencyLocationInRepo = new ArrayList<>(asList(pomModel.getGroupId().split("\\.")));
    dependencyLocationInRepo.add(pomModel.getArtifactId());
    dependencyLocationInRepo.add(pomModel.getVersion());

    Path pathToDependencyLocationInRepo =
        Paths.get(repositoryLocation.getValue(), dependencyLocationInRepo.toArray(new String[0]));
    File artifactLocationInRepoFile = pathToDependencyLocationInRepo.toFile();

    artifactLocationInRepoFile.mkdirs();

    copyFile(dependencyFile, new File(pathToDependencyLocationInRepo.toString(), dependencyFile.getName()), true);

    //Copy the pom without the classifier.
    String pomFileName = dependencyFile.getName().replaceFirst("(.*\\.[0-9]*\\.[0-9]*\\.?[0-9]?).*", "$1") + ".pom";
    copyFile(dependencyFile, new File(pathToDependencyLocationInRepo.toString(), pomFileName), true);
  }

}

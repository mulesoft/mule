/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.maven;

import static java.nio.file.Paths.get;
import static java.util.Collections.unmodifiableList;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.runtime.module.artifact.activation.api.deployable.MuleProjectStructure;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultMuleProjectStructure implements MuleProjectStructure {

  private static final String DEFAULT_SOURCES_DIRECTORY = "src/main";
  private static final String DEFAULT_SOURCES_JAVA_DIRECTORY = "/java";
  private static final String DEFAULT_MULE_DIRECTORY = "/mule";
  private static final String DEFAULT_RESOURCES_DIRECTORY = "/resources";
  private static final String DEFAULT_TEST_RESOURCES_DIRECTORY = "/test/resources";

  private final Path projectFolder;
  private final Path javaDirectory;
  private final Path muleResourcesDirectory;
  private final List<Path> resourcesDirectories;

  public DefaultMuleProjectStructure(Path projectFolder, MavenPomParser parser, boolean includeTestDependencies) {
    this.projectFolder = projectFolder;

    String sourceDirectory = parser.getSourceDirectory();
    this.javaDirectory = get(projectFolder.toAbsolutePath().toString(), sourceDirectory.concat(DEFAULT_SOURCES_JAVA_DIRECTORY));

    this.muleResourcesDirectory =
        get(projectFolder.toAbsolutePath().toString(), parser.getSourceDirectory().concat(DEFAULT_MULE_DIRECTORY));

    List<Path> resourcesDirectories = new ArrayList<>();
    // include test resources if test dependencies have to be considered
    if (includeTestDependencies) {
      resourcesDirectories.add(get(projectFolder.toAbsolutePath().toString(),
                                   parser.getSourceDirectory().concat(DEFAULT_TEST_RESOURCES_DIRECTORY)));
    }
    if (parser.getResourceDirectories().isEmpty()) {
      resourcesDirectories
          .add(get(projectFolder.toAbsolutePath().toString(), parser.getSourceDirectory().concat(DEFAULT_RESOURCES_DIRECTORY)));
    } else {
      parser.getResourceDirectories()
          .forEach(resourceDir -> resourcesDirectories.add(get(projectFolder.toAbsolutePath().toString(), resourceDir)));
    }

    this.resourcesDirectories = unmodifiableList(resourcesDirectories);
  }

  @Override
  public Path getProjectFolder() {
    return projectFolder;
  }

  @Override
  public Path getJavaDirectory() {
    return javaDirectory;
  }

  @Override
  public Path getMuleResourcesDirectory() {
    return muleResourcesDirectory;
  }

  @Override
  public Collection<Path> getResourcesDirectories() {
    return resourcesDirectories;
  }

}

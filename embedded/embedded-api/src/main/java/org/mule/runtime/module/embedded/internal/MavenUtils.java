/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.embedded.internal;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO MULE-11878 - consolidate with other aether usages in mule.
public class MavenUtils {

  private static final String USER_HOME = "user.home";
  private static final String M2_REPO = "/.m2/repository";
  private static String userHome = getProperty(USER_HOME);

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenUtils.class);

  private MavenUtils() {}

  public static File getMavenLocalRepository() {
    String localRepositoryProperty = getProperty("localRepository");
    if (localRepositoryProperty == null) {
      localRepositoryProperty = userHome + M2_REPO;
      LOGGER.debug("System property 'localRepository' not set, using Maven default location: $USER_HOME{}", M2_REPO);
    }

    LOGGER.debug("Using Maven localRepository: '{}'", localRepositoryProperty);
    File mavenLocalRepositoryLocation = new File(localRepositoryProperty);
    if (!mavenLocalRepositoryLocation.exists()) {
      throw new IllegalArgumentException("Maven repository location couldn't be found, please check your configuration");
    }
    return mavenLocalRepositoryLocation;
  }

  public static Model createModelFromPom(File pomLocation) {
    try (FileReader fileReader = new FileReader(pomLocation)) {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(fileReader);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  public static List<URL> loadUrls(PreorderNodeListGenerator nlg) {
    return nlg.getArtifacts(false)
        .stream()
        .map(artifact -> getUrl(artifact.getFile())).collect(Collectors.toList());
  }

  private static URL getUrl(File file) {
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException(format("There was an issue obtaining the URL for the dependency file [%s]",
                                        file.getAbsolutePath()),
                                 e);
    }
  }

}

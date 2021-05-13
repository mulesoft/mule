/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.maven;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.util.function.Function;

import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;

/**
 * Utils static class for creating {@link Artifact}s
 *
 * @since 4.3.1, 4.4.0
 */
public final class ArtifactFactory {

  private static final Logger LOGGER = getLogger(ArtifactFactory.class);

  private ArtifactFactory() {}

  /**
   * Create a new {@link Artifact} from the given pom File.
   */
  public static Artifact createFromPomFile(File pomFile) {
    LOGGER.debug("Reading rootArtifact from pom file: {}", pomFile);
    Model model = MavenModelFactory.createMavenProject(pomFile);

    return new DefaultArtifact(
                               model.getGroupId() != null ? searchingProperties(model, Model::getGroupId)
                                   : searchingProperties(model, m -> m.getParent().getGroupId()),
                               searchingProperties(model, Model::getArtifactId),
                               searchingProperties(model, Model::getPackaging),
                               model.getVersion() != null ? searchingProperties(model, Model::getVersion)
                                   : searchingProperties(model, m -> m.getParent().getVersion()));
  }

  private static String searchingProperties(Model model, Function<Model, String> extractor) {
    String value = extractor.apply(model);
    if (value.startsWith("${")) {
      String propertyKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
      return model.getProperties().getProperty(propertyKey, value);
    }
    return value;
  }

}

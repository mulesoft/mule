/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.maven;

import static org.mule.maven.pom.parser.api.MavenPomParserProvider.discoverProvider;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.maven.pom.parser.api.MavenPomParser;
import org.mule.maven.pom.parser.api.MavenPomParserProvider;
import org.mule.maven.pom.parser.api.model.MavenPomModel;
import org.mule.maven.pom.parser.api.model.PomParentCoordinates;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.util.function.Function;

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
    MavenPomParserProvider provider = discoverProvider();
    MavenPomParser pomParser = provider.createMavenPomParserClient(pomFile.toPath());
    MavenPomModel model = pomParser.getModel();
    return new DefaultArtifact(model.getGroupId() != null ? searchingProperties(pomParser, MavenPomModel::getGroupId)
        : searchingProperties(pomParser, m -> m.getParent().map(PomParentCoordinates::getGroupId)
            .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("GroupId not present in the pom or in the parent pom")))),
                               searchingProperties(pomParser, MavenPomModel::getArtifactId),
                               searchingProperties(pomParser, MavenPomModel::getPackaging),
                               model.getVersion() != null ? searchingProperties(pomParser, MavenPomModel::getVersion)
                                   : searchingProperties(pomParser, m -> m.getParent().map(PomParentCoordinates::getVersion)
                                       .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Version not present in the pom or in the parent pom")))));
  }

  private static String searchingProperties(MavenPomParser parser, Function<MavenPomModel, String> extractor) {
    String value = extractor.apply(parser.getModel());
    if (value.startsWith("${")) {
      String propertyKey = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
      return parser.getProperties().getProperty(propertyKey, value);
    }
    return value;
  }

}

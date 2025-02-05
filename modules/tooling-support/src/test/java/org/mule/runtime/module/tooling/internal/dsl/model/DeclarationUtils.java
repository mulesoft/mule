/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mule.runtime.app.declaration.api.component.location.Location.builderFromStringRepresentation;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ConfigurationElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterizedElementDeclaration;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class DeclarationUtils {

  private DeclarationUtils() {}

  static Optional<ParameterizedElementDeclaration> getParameterElementDeclaration(ArtifactDeclaration artifactDeclaration,
                                                                                  String location) {
    AtomicBoolean isConnection = new AtomicBoolean(false);
    if (location.endsWith("/connection")) {
      isConnection.set(true);
      location = location.split("/connection")[0];
    }
    return artifactDeclaration.<ParameterizedElementDeclaration>findElement(builderFromStringRepresentation(location).build())
        .map(d -> isConnection.get() ? ((ConfigurationElementDeclaration) d).getConnection().orElse(null) : d);
  }

  static void removeParameter(ArtifactDeclaration artifactDeclaration,
                              String ownerLocation,
                              String parameterName) {
    if (!getParameterElementDeclaration(artifactDeclaration, ownerLocation)
        .map(owner -> owner.getParameterGroups().stream()
            .filter(pg -> pg.getParameters().removeIf(p -> p.getName().equals(parameterName))).findAny())
        .orElseThrow(() -> new RuntimeException("Location not found")).isPresent()) {
      throw new RuntimeException("Could not remove parameter from component");
    }
  }

  static void modifyParameter(ArtifactDeclaration artifactDeclaration,
                              String ownerLocation,
                              String parameterName,
                              Consumer<ParameterElementDeclaration> parameterConsumer) {
    getParameterElementDeclaration(artifactDeclaration, ownerLocation)
        .map(
             owner -> owner.getParameterGroups()
                 .stream()
                 .flatMap(pg -> pg.getParameters().stream())
                 .filter(p -> p.getName().equals(parameterName))
                 .findAny()
                 .map(fp -> {
                   parameterConsumer.accept(fp);
                   return EMPTY; // Needed to avoid exception
                 })
                 .orElseThrow(() -> new RuntimeException("Could not find parameter to modify")))
        .orElseThrow(() -> new RuntimeException("Location not found"));
  }
}

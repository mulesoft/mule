/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.ExternalLibs;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;

import java.util.List;
import java.util.Optional;

final class JavaExtensionDefinitionParserUtils {

  public static List<ExternalLibraryModel> parseExternalLibraryModels(WithAnnotations element) {
    Optional<ExternalLibs> externalLibs = element.getAnnotation(ExternalLibs.class);
    if (externalLibs.isPresent()) {
      return stream(externalLibs.get().value())
          .map(lib -> parseExternalLib(lib))
          .collect(toList());
    } else {
      return element.getAnnotation(ExternalLib.class)
          .map(lib -> singletonList(parseExternalLib(lib)))
          .orElse(emptyList());
    }

  }

  private static ExternalLibraryModel parseExternalLib(ExternalLib externalLibAnnotation) {
    ExternalLibraryModel.ExternalLibraryModelBuilder builder = ExternalLibraryModel.builder()
        .withName(externalLibAnnotation.name())
        .withDescription(externalLibAnnotation.description())
        .withType(externalLibAnnotation.type())
        .isOptional(externalLibAnnotation.optional());

    ifNotBlank(externalLibAnnotation.nameRegexpMatcher(), builder::withRegexpMatcher);
    ifNotBlank(externalLibAnnotation.requiredClassName(), builder::withRequiredClassName);
    ifNotBlank(externalLibAnnotation.coordinates(), builder::withCoordinates);

    return builder.build();
  }

}

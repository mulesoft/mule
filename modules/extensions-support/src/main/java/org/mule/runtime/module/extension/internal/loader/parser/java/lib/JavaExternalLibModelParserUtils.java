/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.lib;

import static org.mule.runtime.api.meta.ExternalLibraryType.DEPENDENCY;
import static org.mule.runtime.api.meta.ExternalLibraryType.JAR;
import static org.mule.runtime.api.meta.ExternalLibraryType.NATIVE;
import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceRepeatableAnnotation;

import org.mule.runtime.api.meta.ExternalLibraryType;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.ExternalLibs;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;

import java.util.LinkedList;
import java.util.List;

/**
 * Utilities for parsing External Libraries definitions on a Java based extension
 *
 * @since 4.5.0
 */
public final class JavaExternalLibModelParserUtils {

  public static List<ExternalLibraryModel> parseExternalLibraryModels(WithAnnotations element) {
    List<ExternalLibraryModel> libraries = new LinkedList<>();

    mapReduceRepeatableAnnotation(element, ExternalLib.class, org.mule.sdk.api.annotation.ExternalLib.class,
                                  container -> ((ExternalLibs) container).value(),
                                  container -> ((org.mule.sdk.api.annotation.ExternalLibs) container).value(),
                                  externalLibAnnotationValueFetcher -> {
                                    ExternalLibraryModel.ExternalLibraryModelBuilder builder = ExternalLibraryModel.builder()
                                        .withName(externalLibAnnotationValueFetcher.getStringValue(ExternalLib::name))
                                        .withDescription(externalLibAnnotationValueFetcher
                                            .getStringValue(ExternalLib::description))
                                        .withType(externalLibAnnotationValueFetcher.getEnumValue(ExternalLib::type))
                                        .isOptional(externalLibAnnotationValueFetcher.getBooleanValue(ExternalLib::optional));

                                    ifNotBlank(externalLibAnnotationValueFetcher.getStringValue(ExternalLib::nameRegexpMatcher),
                                               builder::withRegexpMatcher);
                                    ifNotBlank(externalLibAnnotationValueFetcher.getStringValue(ExternalLib::requiredClassName),
                                               builder::withRequiredClassName);
                                    ifNotBlank(externalLibAnnotationValueFetcher.getStringValue(ExternalLib::coordinates),
                                               builder::withCoordinates);

                                    return builder.build();
                                  },
                                  externalLibAnnotationValueFetcher -> {
                                    ExternalLibraryModel.ExternalLibraryModelBuilder builder = ExternalLibraryModel.builder()
                                        .withName(externalLibAnnotationValueFetcher
                                            .getStringValue(org.mule.sdk.api.annotation.ExternalLib::name))
                                        .withDescription(externalLibAnnotationValueFetcher
                                            .getStringValue(org.mule.sdk.api.annotation.ExternalLib::description))
                                        .withType(of(externalLibAnnotationValueFetcher
                                            .getEnumValue(org.mule.sdk.api.annotation.ExternalLib::type)))
                                        .isOptional(externalLibAnnotationValueFetcher
                                            .getBooleanValue(org.mule.sdk.api.annotation.ExternalLib::optional));

                                    ifNotBlank(externalLibAnnotationValueFetcher
                                        .getStringValue(org.mule.sdk.api.annotation.ExternalLib::nameRegexpMatcher),
                                               builder::withRegexpMatcher);
                                    ifNotBlank(externalLibAnnotationValueFetcher
                                        .getStringValue(org.mule.sdk.api.annotation.ExternalLib::requiredClassName),
                                               builder::withRequiredClassName);
                                    ifNotBlank(externalLibAnnotationValueFetcher
                                        .getStringValue(org.mule.sdk.api.annotation.ExternalLib::coordinates),
                                               builder::withCoordinates);

                                    return builder.build();
                                  })
        .forEach(libraries::add);

    return libraries;
  }

  private static ExternalLibraryType of(org.mule.sdk.api.meta.ExternalLibraryType externalLibraryType) {
    switch (externalLibraryType) {
      case JAR:
        return JAR;
      case NATIVE:
        return NATIVE;
      case DEPENDENCY:
        return DEPENDENCY;
    }
    return null;
  }

  private JavaExternalLibModelParserUtils() {}
}

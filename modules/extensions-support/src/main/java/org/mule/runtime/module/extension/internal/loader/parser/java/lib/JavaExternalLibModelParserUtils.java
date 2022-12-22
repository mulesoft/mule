/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.lib;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.StringUtils.ifNotBlank;
import static org.mule.runtime.extension.internal.loader.util.JavaParserUtils.toMuleApi;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.annotation.ExternalLib;
import org.mule.runtime.extension.api.annotation.ExternalLibs;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Utilities for parsing External Libraries definitions on a Java based extension
 *
 * @since 4.5.0
 */
public final class JavaExternalLibModelParserUtils {

  public static List<ExternalLibraryModel> parseExternalLibraryModels(WithAnnotations element) {
    List<ExternalLibraryModel> libraries = new LinkedList<>();

    parseExternalLibs(element,
                      ExternalLibs.class,
                      libs -> Stream.of(libs.value()).map(lib -> parseExternalLib(lib)).collect(toList()),
                      libraries);

    parseExternalLibs(element,
                      org.mule.sdk.api.annotation.ExternalLibs.class,
                      libs -> Stream.of(libs.value()).map(lib -> parseExternalLib(lib)).collect(toList()),
                      libraries);

    element.getAnnotation(ExternalLib.class)
        .map(lib -> parseExternalLib(lib))
        .ifPresent(libraries::add);

    element.getAnnotation(org.mule.sdk.api.annotation.ExternalLib.class)
        .map(lib -> parseExternalLib(lib))
        .ifPresent(libraries::add);

    return libraries;
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

  private static ExternalLibraryModel parseExternalLib(org.mule.sdk.api.annotation.ExternalLib externalLibAnnotation) {
    ExternalLibraryModel.ExternalLibraryModelBuilder builder = ExternalLibraryModel.builder()
        .withName(externalLibAnnotation.name())
        .withDescription(externalLibAnnotation.description())
        .withType(toMuleApi(externalLibAnnotation.type()))
        .isOptional(externalLibAnnotation.optional());

    ifNotBlank(externalLibAnnotation.nameRegexpMatcher(), builder::withRegexpMatcher);
    ifNotBlank(externalLibAnnotation.requiredClassName(), builder::withRequiredClassName);
    ifNotBlank(externalLibAnnotation.coordinates(), builder::withCoordinates);

    return builder.build();
  }

  private static <A extends Annotation> void parseExternalLibs(WithAnnotations element,
                                                               Class<A> annotationClass,
                                                               Function<A, List<ExternalLibraryModel>> mapper,
                                                               List<ExternalLibraryModel> accumulator) {
    element.getAnnotation(annotationClass)
        .map(mapper)
        .ifPresent(accumulator::addAll);
  }

  private JavaExternalLibModelParserUtils() {}
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.DisplayModel.DisplayModelBuilder;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.LayoutModel.LayoutModelBuilder;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.display.PathModel.Location;
import org.mule.runtime.api.meta.model.display.PathModel.Type;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ParameterLayoutParser extends BaseMuleSdkExtensionModelParser {

  private static final String PARAMETER_LAYOUT_DSL_KEY = "parameter-metadata";
  private static final String DISPLAY_NAME = "displayName";
  private static final String EXAMPLE = "example";
  private static final String SUMMARY = "summary";
  private static final String TEXT = "text";
  private static final String SECRET = "secret";
  private static final String PATH = "path";
  private static final String TYPE = "type";
  private static final String ACCEPTS_URLS = "acceptsUrls";
  private static final String LOCATION = "location";
  private static final String ACCEPTED_FILE_EXTENSIONS = "acceptedFileExtensions";
  private static final String[] EMPTY_ARRAY = {};
  private static final String PLACEMENT = "placement";
  private static final String ORDER = "order";

  private final ComponentAst parameterAst;

  private LayoutModel layoutModel = null;
  private DisplayModel displayModel = null;

  public ParameterLayoutParser(ComponentAst parameterAst) {
    this.parameterAst = parameterAst;
    parseStructure();
  }

  private void parseStructure() {
    getSingleChild(parameterAst, PARAMETER_LAYOUT_DSL_KEY).ifPresent(this::parseParameterMetadata);
  }

  private void parseParameterMetadata(ComponentAst metadataAst) {
    DisplayModelBuilder displayModelBuilder = DisplayModel.builder();
    LayoutModelBuilder layoutModelBuilder = LayoutModel.builder();

    getOptionalParameter(metadataAst, DISPLAY_NAME)
        .ifPresent(displayName -> displayModelBuilder.displayName((String) displayName));
    getOptionalParameter(metadataAst, EXAMPLE).ifPresent(example -> displayModelBuilder.example((String) example));
    getOptionalParameter(metadataAst, SUMMARY).ifPresent(summary -> displayModelBuilder.summary((String) summary));
    getSingleChild(metadataAst, PATH).map(this::parsePathModel).ifPresent(displayModelBuilder::path);

    getOptionalParameter(metadataAst, TEXT).filter(Boolean.class::cast).ifPresent(isText -> layoutModelBuilder.asText());
    // TODO: Check semantic terms...
    getOptionalParameter(metadataAst, SECRET).ifPresent(secret -> layoutModelBuilder.asPassword());

    getSingleChild(metadataAst, PLACEMENT).map(this::parsePlacementOrder).ifPresent(layoutModelBuilder::order);

    displayModel = displayModelBuilder.build();
    layoutModel = layoutModelBuilder.build();
  }

  private Integer parsePlacementOrder(ComponentAst placementAst) {
    return getOptionalParameter(placementAst, ORDER).map(asString -> Integer.valueOf((String) asString)).orElse(-1);
  }

  private PathModel parsePathModel(ComponentAst pathAst) {
    Type type = getOptionalParameter(pathAst, TYPE)
        .map(asString -> Enum.valueOf(Type.class, (String) asString))
        .orElse(Type.ANY);
    Boolean acceptsUrls = getOptionalParameter(pathAst, ACCEPTS_URLS)
        .map(asString -> Boolean.valueOf((String) asString))
        .orElse(false);
    Location location = getOptionalParameter(pathAst, LOCATION)
        .map(asString -> Enum.valueOf(Location.class, (String) asString))
        .orElse(Location.ANY);
    String[] acceptedFileExtensions = getOptionalParameter(pathAst, ACCEPTED_FILE_EXTENSIONS)
        .map(asString -> ((String) asString).split(","))
        .orElse(EMPTY_ARRAY);

    return new PathModel(type, acceptsUrls, location, acceptedFileExtensions);
  }

  public Optional<LayoutModel> getLayoutModel() {
    return ofNullable(layoutModel);
  }

  public Optional<DisplayModel> getDisplayModel() {
    // TODO: Remove this param...
    // String summary = getParameter(parameterAst, "summary");
    // if (!isBlank(summary)) {
    // return of(DisplayModel.builder().summary(summary).build());
    // }
    return ofNullable(displayModel);
  }
}

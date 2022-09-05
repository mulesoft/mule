/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.API_KEY;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.CLIENT_ID;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.CLIENT_SECRET;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.PASSWORD;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.SECRET_TOKEN;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.SECURITY_TOKEN;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.TOKEN_ID;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.TOKEN_URL;
import static org.mule.runtime.extension.internal.semantic.ConnectivityVocabulary.SECRET;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.DisplayModel.DisplayModelBuilder;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.display.LayoutModel.LayoutModelBuilder;
import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.display.PathModel.Location;
import org.mule.runtime.api.meta.model.display.PathModel.Type;
import org.mule.runtime.ast.api.ComponentAst;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parser for the parameter metadata, including the {@link DisplayModel}, the {@link LayoutModel}, and the semantic terms.
 *
 * @since 4.5.0
 */
public class ParameterLayoutParser extends BaseMuleSdkExtensionModelParser {

  private static final String PARAMETER_LAYOUT_DSL_KEY = "parameter-metadata";
  private static final String DISPLAY_NAME = "displayName";
  private static final String EXAMPLE = "example";
  private static final String SUMMARY = "summary";
  private static final String TEXT = "text";
  private static final String SECRET_TYPE = "secret";
  private static final String PATH = "path";
  private static final String TYPE = "type";
  private static final String ACCEPTS_URLS = "acceptsUrls";
  private static final String LOCATION = "location";
  private static final String ACCEPTED_FILE_EXTENSIONS = "acceptedFileExtensions";
  private static final String[] EMPTY_ARRAY = {};
  private static final String ORDER = "order";

  private static final Map<String, String> SECRET_TYPE_TO_SEMANTIC_TERM = new HashMap<>(11);

  static {
    SECRET_TYPE_TO_SEMANTIC_TERM.put("CLIENT_ID", CLIENT_ID);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("CLIENT_SECRET", CLIENT_SECRET);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("TOKEN_ID", TOKEN_ID);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("PASSWORD", PASSWORD);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("TOKEN_URL_TEMPLATE", TOKEN_URL);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("TOKEN_SECRET", SECRET_TOKEN);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("API_KEY", API_KEY);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("SECRET_TOKEN", SECRET_TOKEN);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("SECURITY_TOKEN", SECURITY_TOKEN);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("RSA_PRIVATE_KEY", SECRET);
    SECRET_TYPE_TO_SEMANTIC_TERM.put("SECRET", SECRET);
  }

  private final ComponentAst parameterAst;

  private LayoutModel layoutModel = null;
  private DisplayModel displayModel = null;
  private final Set<String> semanticTerms;

  public ParameterLayoutParser(ComponentAst parameterAst) {
    this.semanticTerms = new HashSet<>(1);
    this.parameterAst = parameterAst;
    parseStructure();
  }

  private void parseStructure() {
    getSingleChild(parameterAst, PARAMETER_LAYOUT_DSL_KEY).ifPresent(this::parseParameterMetadata);
  }

  private void parseParameterMetadata(ComponentAst metadataAst) {
    DisplayModelBuilder displayModelBuilder = DisplayModel.builder();
    LayoutModelBuilder layoutModelBuilder = LayoutModel.builder();

    boolean wasSomeDisplayModelParamSet = setDisplayNameIfNeeded(metadataAst, displayModelBuilder);
    wasSomeDisplayModelParamSet |= setExampleIfNeeded(metadataAst, displayModelBuilder);
    wasSomeDisplayModelParamSet |= setSummaryIfNeeded(metadataAst, displayModelBuilder);
    wasSomeDisplayModelParamSet |= setPathIfNeeded(metadataAst, displayModelBuilder);

    boolean wasSomeLayoutModelParamSet = setTextIfNeeded(metadataAst, layoutModelBuilder);
    wasSomeLayoutModelParamSet |= setSecretIfNeeded(metadataAst, layoutModelBuilder);
    wasSomeLayoutModelParamSet |= setOrderIfNeeded(metadataAst, layoutModelBuilder);

    if (wasSomeDisplayModelParamSet) {
      displayModel = displayModelBuilder.build();
    }

    if (wasSomeLayoutModelParamSet) {
      layoutModel = layoutModelBuilder.build();
    }
  }

  private boolean setTextIfNeeded(ComponentAst metadataAst, LayoutModelBuilder layoutModelBuilder) {
    Optional<Boolean> isText = getOptionalParameter(metadataAst, TEXT);
    if (isText.isPresent()) {
      if (isText.get().booleanValue()) {
        layoutModelBuilder.asText();
      }
      return true;
    } else {
      return false;
    }
  }

  private boolean setSecretIfNeeded(ComponentAst metadataAst, LayoutModelBuilder layoutModelBuilder) {
    Optional<String> secret = getOptionalParameter(metadataAst, SECRET_TYPE);
    if (secret.isPresent()) {
      semanticTerms.add(toSemanticTerm(secret.get()));
      layoutModelBuilder.asPassword();
      return true;
    } else {
      return false;
    }
  }

  private String toSemanticTerm(String secretType) {
    return SECRET_TYPE_TO_SEMANTIC_TERM.getOrDefault(secretType, SECRET);
  }

  private boolean setOrderIfNeeded(ComponentAst metadataAst, LayoutModelBuilder layoutModelBuilder) {
    Optional<Integer> order = getOptionalParameter(metadataAst, ORDER);
    if (order.isPresent()) {
      layoutModelBuilder.order(order.get());
      return true;
    } else {
      return false;
    }
  }

  private boolean setDisplayNameIfNeeded(ComponentAst metadataAst, DisplayModelBuilder displayModelBuilder) {
    Optional<String> displayName = getOptionalParameter(metadataAst, DISPLAY_NAME);
    if (displayName.isPresent() && !isBlank(displayName.get())) {
      displayModelBuilder.displayName(displayName.get());
      return true;
    } else {
      return false;
    }
  }

  private boolean setExampleIfNeeded(ComponentAst metadataAst, DisplayModelBuilder displayModelBuilder) {
    Optional<String> example = getOptionalParameter(metadataAst, EXAMPLE);
    if (example.isPresent() && !isBlank(example.get())) {
      displayModelBuilder.example(example.get());
      return true;
    } else {
      return false;
    }
  }

  private boolean setSummaryIfNeeded(ComponentAst metadataAst, DisplayModelBuilder displayModelBuilder) {
    Optional<String> summary = getOptionalParameter(metadataAst, SUMMARY);
    if (summary.isPresent() && !isBlank(summary.get())) {
      displayModelBuilder.summary(summary.get());
      return true;
    } else {
      return false;
    }
  }

  private boolean setPathIfNeeded(ComponentAst metadataAst, DisplayModelBuilder displayModelBuilder) {
    Optional<PathModel> pathModel = getSingleChild(metadataAst, PATH).map(this::parsePathModel);
    if (pathModel.isPresent()) {
      displayModelBuilder.path(pathModel.get());
      return true;
    } else {
      return false;
    }
  }

  private PathModel parsePathModel(ComponentAst pathAst) {
    Type type = getOptionalParameter(pathAst, TYPE)
        .map(asString -> Enum.valueOf(Type.class, (String) asString))
        .orElse(Type.ANY);
    Boolean acceptsUrls = getOptionalParameter(pathAst, ACCEPTS_URLS)
        .map(Boolean.class::cast)
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
    return ofNullable(displayModel);
  }

  public Set<String> getSemanticTerms() {
    return semanticTerms;
  }
}

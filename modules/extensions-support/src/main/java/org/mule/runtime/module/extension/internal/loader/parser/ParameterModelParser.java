/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.List;
import java.util.Optional;

public interface ParameterModelParser {

  String getName();

  String getDescription();

  MetadataType getType();

  ParameterRole getRole();

  ExpressionSupport getExpressionSupport();

  Optional<LayoutModel> getLayoutModel();

  Optional<ParameterDslConfiguration> getDslConfiguration();

  List<StereotypeModel> getAllowedStereotypes();

  List<ModelProperty> getAdditionalModelProperties();
}

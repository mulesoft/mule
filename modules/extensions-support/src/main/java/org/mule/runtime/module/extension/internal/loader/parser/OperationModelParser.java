/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;

import java.util.List;
import java.util.Optional;

public interface OperationModelParser {

  String getName();

  String getDescription();

  boolean isIgnored();

  boolean isScope();

  boolean isConnected();

  boolean hasConfig();

  boolean isNonBlocking();

  Optional<MediaTypeModelProperty> getMediaTypeModelProperty();

  List<ModelProperty> getAdditionalModelProperties();
}

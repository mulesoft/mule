/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.loader.parser.OutputModelParser;

/**
 * Default implementation for {@link OutputModelParser}.
 * <p>
 * This implementation is syntax agnostic.
 *
 * @since 4.5.0
 */
public class DefaultOutputModelParser implements OutputModelParser {

  private final MetadataType type;
  private final boolean isDynamic;

  public DefaultOutputModelParser(MetadataType type, boolean isDynamic) {
    this.type = type;
    this.isDynamic = isDynamic;
  }

  @Override
  public MetadataType getType() {
    return type;
  }

  @Override
  public boolean isDynamic() {
    return isDynamic;
  }
}

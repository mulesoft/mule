/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.VOID_TYPE;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.module.extension.internal.loader.parser.OutputModelParser;

/**
 * Represents a void output type
 *
 * @since 4.5.0
 */
public class VoidOutputModelParser implements OutputModelParser {

  public static OutputModelParser INSTANCE = new VoidOutputModelParser();

  @Override
  public MetadataType getType() {
    return VOID_TYPE;
  }

  @Override
  public boolean isDynamic() {
    return false;
  }
}

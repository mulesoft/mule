/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.VOID_TYPE;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.loader.parser.OutputModelParser;

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

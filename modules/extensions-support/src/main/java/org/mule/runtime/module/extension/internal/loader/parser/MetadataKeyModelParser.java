/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;

import java.util.Optional;

/**
 * Parses the syntactic definition of the metadata keys id so that the semantics reflected in it can be extracted in a uniform
 * way, regardless of the actual syntax used by the extension developer.
 *
 * @see ExtensionModelParser
 * @since 4.5.0
 */
public interface MetadataKeyModelParser {

  /**
   * @return {@code true} if this parser parse a Key resolver different from {@link NullMetadataResolver} and
   *         {@link org.mule.runtime.extension.api.metadata.NullMetadataResolver}
   */
  boolean hasKeyIdResolver();

  /**
   * @return {@code true} if this parser parse a Key resolver is an instance of {@link PartialTypeKeysResolver} or
   *         {@link org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver}
   */
  boolean isPartialKeyResolver();

  /**
   * @return an instance of {@link TypeKeysResolver}
   */
  TypeKeysResolver getKeyResolver();

  /**
   * @return an instance the {@link MetadataType}
   */
  MetadataType getMetadataType();

  /**
   * @return the parameter name of the metadata key
   */
  String getParameterName();
}
/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.extension;

import static java.lang.String.format;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

/**
 * {@link ChainInputTypeResolver} for the ForEach and ParallelForEach scopes.
 *
 * @since 4.8.0
 */
public class ForEachChainInputTypeResolver implements ChainInputTypeResolver {

  private static final String PARAMETER_NAME = "collection";

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context) throws MetadataResolvingException {
    MessageMetadataType messageMetadataType = context.getInputMessageMetadataType();
    MessageMetadataTypeBuilder chainMessageMetadataTypeBuilder = MessageMetadataType.builder();

    messageMetadataType.getAttributesType()
        .ifPresent(chainMessageMetadataTypeBuilder::attributes);

    chainMessageMetadataTypeBuilder.payload(resolveChainPayloadType(context.getParameterResolvedType(PARAMETER_NAME)));

    return chainMessageMetadataTypeBuilder.build();
  }

  @Override
  public String getCategoryName() {
    return "FOREACH";
  }

  @Override
  public String getResolverName() {
    return "FOREACH_CHAIN_INPUT";
  }

  private MetadataType resolveChainPayloadType(MetadataType collectionType) {
    if (!(collectionType instanceof ArrayType)) {
      throw new IllegalArgumentException(format("Expected a collection from parameter `%s`", PARAMETER_NAME));
    }
    return ((ArrayType) collectionType).getType();
  }
}

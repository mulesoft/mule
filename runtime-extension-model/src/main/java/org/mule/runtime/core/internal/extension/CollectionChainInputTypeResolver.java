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
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

/**
 * {@link ChainInputTypeResolver} implementation that resolves to the type of the elements of a given parameter that is expected
 * to resolve to an array type.
 *
 * @since 4.7.0
 */
public class CollectionChainInputTypeResolver implements ChainInputTypeResolver {

  private final String collectionParameterName;

  public CollectionChainInputTypeResolver(String collectionParameterName) {
    this.collectionParameterName = collectionParameterName;
  }

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context) {
    MessageMetadataType messageMetadataType = context.getInputMessageMetadataType();
    MessageMetadataTypeBuilder chainMessageMetadataTypeBuilder = MessageMetadataType.builder();

    messageMetadataType.getAttributesType()
        .ifPresent(chainMessageMetadataTypeBuilder::attributes);

    // TODO: actually evaluate the "collection" parameter once it actually returns the resolved type
    // chainMessageMetadataTypeBuilder.payload(resolveChainPayloadType(context.getParameterResolvedType(collectionParameterName)));
    messageMetadataType.getPayloadType()
        .map(this::resolveChainPayloadType)
        .ifPresent(chainMessageMetadataTypeBuilder::payload);

    return chainMessageMetadataTypeBuilder.build();
  }

  @Override
  public String getCategoryName() {
    return "SCOPE_COLLECTION";
  }

  @Override
  public String getResolverName() {
    return "SCOPE_INPUT_COLLECTION";
  }

  private MetadataType resolveChainPayloadType(MetadataType collectionType) {
    if (!(collectionType instanceof ArrayType)) {
      throw new IllegalArgumentException(format("Collection Expression of parameter `%s` does not resolve to a collection",
                                                collectionParameterName));
    }
    return ((ArrayType) collectionType).getType();
  }
}

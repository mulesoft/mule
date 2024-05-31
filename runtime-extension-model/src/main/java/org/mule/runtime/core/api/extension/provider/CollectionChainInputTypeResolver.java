/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension.provider;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_METADATA_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;

import static java.lang.String.format;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.metadata.message.api.MessageMetadataTypeBuilder;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ChainInputTypeResolver} implementation that resolves to the type of the elements of the collection from
 * {@link ChainInputMetadataContext#getInputMessageMetadataType()}
 *
 * @since 4.7.0
 */
// TODO: move away from here
public class CollectionChainInputTypeResolver implements ChainInputTypeResolver {

  public static CollectionChainInputTypeResolver INSTANCE = new CollectionChainInputTypeResolver();

  private CollectionChainInputTypeResolver() {}

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context) {
    MessageMetadataType messageMetadataType = context.getInputMessageMetadataType();
    MessageMetadataTypeBuilder chainMessageMetadataTypeBuilder = MessageMetadataType.builder();

    messageMetadataType.getAttributesType()
        .ifPresent(chainMessageMetadataTypeBuilder::attributes);

    // TODO: actually evaluate the "collection" parameter
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
    return (collectionType instanceof ArrayType) ? ((ArrayType) collectionType).getType()
        : new BaseTypeBuilder(JAVA).anyType().build();
  }
/*
  private MetadataType resolveCollectionType(String expression, ComponentLocation opLocation) {
    TypeBindings bindings = getBindings(messageTracker.getCurrentPayload(), messageTracker.getCurrentAttributes(),
                                        variablesTracker.getCurrentVariables());
    List<String> errorsFound = new ArrayList<>();
    MetadataType arrayType =
      expressionLanguageMetadataService.getOutputType(bindings, expression, createMessageCallback(errorsFound));
    if (errorsFound.isEmpty() && (arrayType instanceof ArrayType)) {
      return ((ArrayType) arrayType).getType();
    } else {
      outputs.put(opLocation,
                  failure(newFailure()
                            .withMessage(format("Collection Expression (%s) at location %s does not resolve to a collection",
                                                expression, opLocation.getLocation()))
                            .withFailureCode(NO_DYNAMIC_METADATA_AVAILABLE).onComponent()));
      success = false;
      return null;
    }
  }
 */
}

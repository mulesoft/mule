/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import static org.mule.runtime.api.metadata.resolving.FailureCode.UNKNOWN;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;

import java.util.Optional;
import java.util.function.Function;

/**
 * Pass Through {@link OutputTypeResolver} implementation for routes or scope's inner chains that outputs a message with
 * the same payload and attributes types that were received.
 * <p>
 * Propagates the inner chain's resolved payload type.
 *
 * @since 4.7
 */
public class PassThroughChainOutputTypeResolver implements OutputTypeResolver<Void>, AttributesTypeResolver<Void> {

  public static final PassThroughChainOutputTypeResolver INSTANCE = new PassThroughChainOutputTypeResolver();

  private PassThroughChainOutputTypeResolver() {}

  @Override
  public String getCategoryName() {
    return "SCOPE_PASSTHROUGH";
  }

  @Override
  public String getResolverName() {
    return "SCOPE_OUTPUT_PASSTHROUGH";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Void key) throws MetadataResolvingException, ConnectionException {
    return passthrough(context, MessageMetadataType::getPayloadType)
        .orElseThrow(() -> new MetadataResolvingException("Resolved output payload not found.", UNKNOWN));
  }

  @Override
  public MetadataType getAttributesType(MetadataContext context, Void key)
      throws MetadataResolvingException, ConnectionException {
    return passthrough(context, MessageMetadataType::getAttributesType)
        .orElseThrow(() -> new MetadataResolvingException("Resolved output attributes not found.", UNKNOWN));
  }

  private Optional<MetadataType> passthrough(MetadataContext context,
                                             Function<MessageMetadataType, Optional<MetadataType>> extractor) {
    return context.getScopeOutputMetadataContext()
        .flatMap(ctx -> extractor.apply(ctx.getInnerChainOutputMessageType().get()));
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

public class NullChainInputTypeResolver implements ChainInputTypeResolver {

  public static ChainInputTypeResolver NULL_INSTANCE = new NullChainInputTypeResolver();

  private NullChainInputTypeResolver() {}

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    MetadataType anyType = context.getTypeBuilder().anyType().build();
    return MessageMetadataType.builder()
        .payload(anyType)
        .attributes(anyType)
        .build();
  }

  @Override
  public String getCategoryName() {
    return "UNKNOWN";
  }

  @Override
  public String getResolverName() {
    return null;
  }
}

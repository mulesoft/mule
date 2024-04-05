/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

public class TestChainInputTypeResolver implements ChainInputTypeResolver {

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    return MessageMetadataType.builder()
        .payload(context.getParameterResolvedType("jsonValue"))
        .attributes(context.getTypeBuilder().voidType().build())
        .build();

  }

  @Override
  public String getCategoryName() {
    return "scope";
  }

  @Override
  public String getResolverName() {
    return "scopeInput";
  }
}

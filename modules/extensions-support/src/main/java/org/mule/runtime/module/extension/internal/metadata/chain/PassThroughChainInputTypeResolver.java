/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata.chain;

import org.mule.metadata.message.api.MessageMetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.sdk.api.metadata.ChainInputMetadataContext;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

public class PassThroughChainInputTypeResolver implements ChainInputTypeResolver {

  public static PassThroughChainInputTypeResolver INSTANCE = new PassThroughChainInputTypeResolver();

  private PassThroughChainInputTypeResolver() {}

  @Override
  public MessageMetadataType getChainInputMetadataType(ChainInputMetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    return context.getInputMessageMetadataType();
  }

  @Override
  public String getCategoryName() {
    return "SCOPE_PASSTHROUGH";
  }

  @Override
  public String getResolverName() {
    return "SCOPE_INPUT_PASSTHROUGH";
  }
}

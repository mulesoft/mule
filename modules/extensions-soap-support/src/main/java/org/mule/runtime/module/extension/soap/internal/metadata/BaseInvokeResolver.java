/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.soap.internal.metadata;

import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.extension.api.soap.WebServiceTypeKey;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.soap.api.client.SoapClient;

/**
 * Base type resolver for the Soap Connect invoke operation.
 *
 * @since 4.0
 */
abstract class BaseInvokeResolver implements NamedTypeResolver {

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCategoryName() {
    return "InvokeOperationCategory";
  }

  SoapClient getClient(MetadataContext context, WebServiceTypeKey key)
      throws MetadataResolvingException, ConnectionException {
    ForwardingSoapClient connection = getConnection(context);
    return connection.getSoapClient(key.getService());
  }

  ForwardingSoapClient getConnection(MetadataContext context)
      throws MetadataResolvingException, ConnectionException {
    return context.<ForwardingSoapClient>getConnection()
        .orElseThrow(() -> new MetadataResolvingException("Cannot obtain connection", CONNECTION_FAILURE));
  }
}

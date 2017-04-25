/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.services.soap.api.client.SoapClient;

import java.util.Set;

/**
 * {@link TypeKeysResolver} implementation for the {@link ConsumeOperation}, retrieves a metadata key for each operation available
 * to be performed for the given {@link SoapClient}.
 *
 * @since 4.0
 */
public class OperationKeysResolver extends BaseWscResolver implements TypeKeysResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return getMetadataResolver(context).getAvailableOperations().stream().map(ope -> newKey(ope).build()).collect(toSet());
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata;

import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.ws.internal.ConsumeOperation;
import org.mule.extension.ws.internal.connection.WscConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.util.collection.ImmutableSetCollector;

import java.util.List;
import java.util.Set;

/**
 * {@link TypeKeysResolver} implementation for the {@link ConsumeOperation}, retrieves a metadata key for each
 * operation available to be performed for the given {@link WscConnection}.
 *
 * @since 4.0
 */
public class OperationKeysResolver extends BaseWscResolver implements TypeKeysResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    WscConnection connection = getConnection(context);
    List<String> operations = connection.getWsdlIntrospecter().getOperationNames();
    return operations.stream().map(ope -> newKey(ope).build()).collect(new ImmutableSetCollector<>());
  }
}

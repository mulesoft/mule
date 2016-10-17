/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.api.metadata;

import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.extension.ws.api.WscConnection;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.util.collection.ImmutableSetCollector;

import java.util.List;
import java.util.Set;

public class OperationKeysResolver extends BaseWscResolver implements TypeKeysResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    WscConnection connection = getConnection(context);
    List<String> operations = connection.getWsdlIntrospecter().getOperationNames();
    return operations.stream().map(ope -> newKey(ope).build()).collect(new ImmutableSetCollector<>());
  }
}

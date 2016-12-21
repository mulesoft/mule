/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.tck.testmodels.fruit.Apple;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class AppleTypesResolver implements TypeKeysResolver, OutputTypeResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return ImmutableSet.of(MetadataKeyBuilder.newKey("APPLE").build());
  }

  @Override
  public String getResolverName() {
    return "AppleTypesResolver";
  }

  @Override
  public String getCategoryName() {
    return "AppleKeys";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return context.getTypeLoader().load(Apple.class);
  }
}

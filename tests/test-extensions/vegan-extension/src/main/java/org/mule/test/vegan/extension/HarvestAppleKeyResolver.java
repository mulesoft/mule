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
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.testmodels.fruit.Apple;

import com.google.common.collect.ImmutableSet;

import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HarvestAppleKeyResolver implements TypeKeysResolver, OutputTypeResolver {

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    InputStream keysOverride = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream("HarvestAppleKeyResolver.keys");
    if (keysOverride != null) {
      try {
        String keysCsv = IOUtils.toString(keysOverride);
        if (!keysCsv.isEmpty()) {
          return Stream.of(keysCsv.split(","))
              .map(name -> MetadataKeyBuilder.newKey(name).build())
              .collect(Collectors.toSet());
        }
      } catch (Exception e) {
        throw new MetadataResolvingException(e.getMessage(), FailureCode.INVALID_METADATA_KEY, e);
      }
    }

    return ImmutableSet.of(MetadataKeyBuilder.newKey("HARVESTED").build());
  }

  @Override
  public String getResolverName() {
    return "HarvestAppleKeyResolver";
  }

  @Override
  public String getCategoryName() {
    return "HarvestedKeys";
  }

  @Override
  public MetadataType getOutputType(MetadataContext context, Object key)
      throws MetadataResolvingException, ConnectionException {
    return context.getTypeLoader().load(Apple.class);
  }
}

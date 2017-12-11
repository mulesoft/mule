/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.test.metadata.extension.LocationKey;

public class TestPartialMultiLevelKeyResolver extends TestMultiLevelKeyResolver
    implements PartialTypeKeysResolver<LocationKey> {

  @Override
  public MetadataKey resolveChilds(MetadataContext context, LocationKey partial)
      throws MetadataResolvingException, ConnectionException {

    // This is incomplete but you get the idea
    if (AMERICA.equalsIgnoreCase(partial.getContinent())) {

      if (ARGENTINA.equalsIgnoreCase(partial.getCountry())) {
        return newKey(AMERICA).withDisplayName(AMERICA)
            .withChild(newKey(ARGENTINA)
                .withChild(newKey(BUENOS_AIRES))
                .withChild(newKey(LA_PLATA)))
            .build();
      }

      return buildAmericaKey();

    } else if (EUROPE.equalsIgnoreCase(partial.getContinent())) {
      return buildEuropeKey();

    } else {
      throw new MetadataResolvingException("Invalid Continent", FailureCode.INVALID_METADATA_KEY);
    }
  }

  public static MetadataKey buildEuropeKey() {
    return newKey(EUROPE).withDisplayName(EUROPE).withChild(newKey(FRANCE).withChild(newKey(PARIS))).build();
  }

  public static MetadataKey buildAmericaKey() {
    return newKey(AMERICA).withDisplayName(AMERICA)
        .withChild(newKey(ARGENTINA).withChild(newKey(BUENOS_AIRES)).withChild(newKey(LA_PLATA)))
        .withChild(newKey(USA).withDisplayName(USA_DISPLAY_NAME).withChild(newKey(SAN_FRANCISCO))).build();
  }

  @Override
  public String getResolverName() {
    return "PartialTestMultiLevelKeyResolver";
  }

}

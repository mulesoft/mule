/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static com.google.common.collect.Sets.newHashSet;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.APPLICATION_JAVA_MIME_TYPE;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.test.metadata.extension.LocationKey;

import java.util.Set;

public class TestMultiLevelKeyResolver implements TypeKeysResolver, InputTypeResolver<LocationKey> {

  public static final String ERROR_MESSAGE = "LocationKey type metadata key was not injected properly in the NamedTypeResolver";

  // continents
  public static final String AMERICA = "AMERICA";
  public static final String EUROPE = "EUROPE";

  // countries
  public static final String FRANCE = "FRANCE";
  public static final String ARGENTINA = "ARGENTINA";
  public static final String USA = "USA";
  public static final String USA_DISPLAY_NAME = "United States";

  // cities
  public static final String BUENOS_AIRES = "BA";
  public static final String LA_PLATA = "LPLT";
  public static final String PARIS = "PRS";
  public static final String SAN_FRANCISCO = "SFO";


  @Override
  public String getCategoryName() {
    return "MetadataExtensionResolver";
  }

  @Override
  public String getResolverName() {
    return "TestMultiLevelKeyResolver";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, LocationKey key)
      throws MetadataResolvingException, ConnectionException {
    checkLocationKey(key);
    final ObjectTypeBuilder objectBuilder =
        BaseTypeBuilder.create(new MetadataFormat(key.toString(), key.toString(), APPLICATION_JAVA_MIME_TYPE)).objectType();
    objectBuilder.addField().key("CONTINENT").value().stringType();
    objectBuilder.addField().key("COUNTRY").value().stringType();
    objectBuilder.addField().key("CITY").value().stringType();
    return objectBuilder.build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    return newHashSet(buildAmericaKey(), buildEuropeKey());
  }

  public static MetadataKey buildEuropeKey() {
    return newKey(EUROPE).withDisplayName(EUROPE).withChild(newKey(FRANCE).withChild(newKey(PARIS))).build();
  }

  public static MetadataKey buildAmericaKey() {
    return newKey(AMERICA).withDisplayName(AMERICA)
        .withChild(newKey(ARGENTINA).withChild(newKey(BUENOS_AIRES)).withChild(newKey(LA_PLATA)))
        .withChild(newKey(USA).withDisplayName(USA_DISPLAY_NAME).withChild(newKey(SAN_FRANCISCO))).build();
  }

  private void checkLocationKey(LocationKey key) throws MetadataResolvingException {
    boolean injectedProperly =
        key != null && key.getContinent().equals(AMERICA) && key.getCountry().equals(USA) && key.getCity().equals(SAN_FRANCISCO);

    if (!injectedProperly) {
      throw new MetadataResolvingException(ERROR_MESSAGE, FailureCode.INVALID_METADATA_KEY);
    }
  }
}

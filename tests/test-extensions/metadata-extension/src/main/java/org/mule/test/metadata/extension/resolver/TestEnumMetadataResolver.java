/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.test.metadata.extension.model.animals.AnimalClade;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.animals.SwordFish;

public class TestEnumMetadataResolver implements InputTypeResolver<AnimalClade> {

  @Override
  public String getCategoryName() {
    return "EnumMetadataResolver";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, AnimalClade key)
      throws MetadataResolvingException, ConnectionException {
    switch (key) {
      case MAMMAL:
        return context.getTypeLoader().load(Bear.class);
      case FISH:
        return context.getTypeLoader().load(SwordFish.class);
      default:
        throw new MetadataResolvingException("Cannot identify animal", INVALID_METADATA_KEY);
    }
  }
}

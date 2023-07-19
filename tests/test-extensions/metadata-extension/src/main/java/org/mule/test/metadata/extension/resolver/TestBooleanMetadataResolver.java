/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.runtime.api.metadata.resolving.FailureCode.INVALID_METADATA_KEY;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.test.metadata.extension.model.animals.SwordFish;

public class TestBooleanMetadataResolver implements InputTypeResolver<Boolean> {

  public static final ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Override
  public String getCategoryName() {
    return "BooleanMetadataResolver";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, Boolean key)
      throws MetadataResolvingException, ConnectionException {
    if (key) {
      return loader.load(SwordFish.class);
    }
    throw new MetadataResolvingException("false key", INVALID_METADATA_KEY);
  }
}

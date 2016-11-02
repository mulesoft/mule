/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

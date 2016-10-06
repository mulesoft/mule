/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

public class TestInputResolver implements InputTypeResolver<String> {

  @Override
  public String getCategoryName() {
    return "TestResolvers";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key)
      throws MetadataResolvingException, ConnectionException {
    return TestMetadataResolverUtils.getMetadata(key);
  }
}

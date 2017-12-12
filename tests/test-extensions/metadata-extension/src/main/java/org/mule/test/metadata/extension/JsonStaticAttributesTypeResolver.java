/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.metadata.extension;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.metadata.resolving.AttributesStaticTypeResolver;
import org.mule.test.metadata.extension.resolver.JsonInputStaticTypeResolver;

public class JsonStaticAttributesTypeResolver extends AttributesStaticTypeResolver {

  @Override
  public MetadataType getStaticMetadata() {
    return new JsonInputStaticTypeResolver().getStaticMetadata();
  }
}

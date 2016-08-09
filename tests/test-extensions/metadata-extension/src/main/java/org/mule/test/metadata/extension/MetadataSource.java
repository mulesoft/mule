/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.MetadataConnection.PERSON;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputWithAttributesResolverWithKeyResolver;

import java.util.Map;


@MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
    contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
    outputResolver = TestContentAndOutputWithAttributesResolverWithKeyResolver.class,
    attributesResolver = TestContentAndOutputWithAttributesResolverWithKeyResolver.class)
public class MetadataSource extends Source<Map<String, Object>, StringAttributes> {

  @MetadataKeyId
  @Parameter
  public String type;

  @Override
  public void start() {
    if (!type.equals(PERSON)) {
      throw new RuntimeException(String.format("Invalid MetadataKey with value [%s], the key should be [%s]", type, PERSON));
    }
  }

  @Override
  public void stop() {

  }
}

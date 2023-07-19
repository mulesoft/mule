/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver;
import org.mule.test.metadata.extension.resolver.TestInputAndOutputWithAttributesResolverWithKeyResolver;

import java.util.Map;


@MetadataScope(keysResolver = TestInputOutputSourceResolverWithKeyResolver.class,
    outputResolver = TestInputAndOutputWithAttributesResolverWithKeyResolver.class)
public class MetadataSource extends Source<Map<String, Object>, StringAttributes> {

  public static boolean STARTED = false;

  @MetadataKeyId
  @Parameter
  public String type;

  @Connection
  private ConnectionProvider<MetadataConnection> connection;

  @Override
  public void onStart(SourceCallback<Map<String, Object>, StringAttributes> sourceCallback) throws MuleException {
    STARTED = true;
    if (!type.equals(PERSON)) {
      throw new RuntimeException(String.format("Invalid MetadataKey with value [%s], the key should be [%s]", type, PERSON));
    }
  }

  @Override
  public void onStop() {
    STARTED = false;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tooling.extensions.metadata.api.source;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tooling.extensions.metadata.internal.metadata.MultiLevelPartialTypeKeysOutputTypeResolver;
import org.mule.tooling.extensions.metadata.api.parameters.LocationKey;

import java.util.Map;

@MetadataScope(keysResolver = MultiLevelPartialTypeKeysOutputTypeResolver.class,
    outputResolver = MultiLevelPartialTypeKeysOutputTypeResolver.class)
public class MetadataSourceWithMultilevel extends Source<Map<String, Object>, StringAttributes> {

  @MetadataKeyId
  @ParameterGroup(name = "Location")
  public LocationKey key;

  @Override
  public void onStart(SourceCallback<Map<String, Object>, StringAttributes> sourceCallback) throws MuleException {
  }

  @Override
  public void onStop() {

  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.testmodels.fruit.Fruit;

@MetadataScope(outputResolver = FruitMetadataResolver.class)
public class PaulMcCartneySource extends Source<Fruit, Object> {

  @Config
  private Object config;

  @Override
  public void onStart(SourceCallback<Fruit, Object> sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}

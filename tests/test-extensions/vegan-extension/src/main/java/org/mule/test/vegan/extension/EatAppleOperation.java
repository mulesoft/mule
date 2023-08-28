/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EatAppleOperation {

  @OutputResolver(output = AppleTypesResolver.class)
  @MediaType(value = ANY, strict = false)
  public Object eatApple(@org.mule.sdk.api.annotation.param.Connection Apple apple,
                         @org.mule.sdk.api.annotation.param.Config AppleConfig config, @MetadataKeyId @Optional String key) {
    apple.bite();
    return apple;
  }

  public List<String> getAllApples(@Content Map<String, Apple> apples) {
    return apples.keySet().stream().collect(Collectors.toList());
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;


import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.tck.testmodels.fruit.Apple;

@MetadataScope(outputResolver = AppleTypesResolver.class,
    keysResolver = AppleTypesResolver.class)
public class EatAppleOperation {

  public Apple eatApple(@Connection Apple apple, @UseConfig AppleConfig config, @MetadataKeyId @Optional String key) {
    apple.bite();
    return apple;
  }
}

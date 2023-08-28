/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;


import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;

@Extension(name = VeganExtension.VEGAN)
@Configurations({AppleConfig.class, BananaConfig.class, KiwiConfig.class, PeachConfig.class, PearConfig.class, GrapeConfig.class})
@Operations(VeganFidelityOperation.class)
@SubTypeMapping(baseType = FarmedFood.class, subTypes = {RottenFood.class, HealthyFood.class})
@MetadataScope(keysResolver = AppleTypesResolver.class)
public class VeganExtension {

  public static final String VEGAN = "vegan";
  public static final String APPLE = "apple-config";
  public static final String BANANA = "banana-config";
  public static final String KIWI = "kiwi-config";
  public static final String PEACH = "peach-config";

}


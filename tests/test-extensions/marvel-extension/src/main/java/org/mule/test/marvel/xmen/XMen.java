/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.xmen;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;

@Configuration(name = XMen.CONFIG_NAME)
@Operations(WeaponXOperations.class)
@Sources({CerebroDetectNewMutants.class, MagnetoMutantSummon.class, MagnetoBrotherhood.class})
public class XMen {

  public static final String CONFIG_NAME = "x-men";

}

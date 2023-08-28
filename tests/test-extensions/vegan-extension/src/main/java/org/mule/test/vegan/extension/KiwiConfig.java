/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import static org.mule.test.vegan.extension.VeganExtension.KIWI;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@Configuration(name = KIWI)
@Operations(EatKiwiOperation.class)
@ConnectionProviders(VeganKiwiConnectionProvider.class)
public class KiwiConfig {

}

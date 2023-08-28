/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@ConnectionProviders(GrapeConnProvider.class)
@Configuration(name = "grape")
@Operations(GrapeOperations.class)
public class GrapeConfig {


}

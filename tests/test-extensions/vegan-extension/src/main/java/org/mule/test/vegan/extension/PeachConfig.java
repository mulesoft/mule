/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.vegan.extension;

import static org.mule.test.vegan.extension.VeganExtension.PEACH;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@Configuration(name = PEACH)
// No operations defined on purpose
@ConnectionProviders(VeganPeachConnectionProvider.class)
@Sources({HarvestPeachesSource.class})
public class PeachConfig {

}

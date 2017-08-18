/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration(name = BANANA)
@Operations({EatBananaOperation.class, SpreadVeganismOperation.class, FlowListenerBananaOperations.class,
    FruitOperationsWithConfigOverride.class})
@ConnectionProviders(VeganBananaConnectionProvider.class)
@Sources(PaulMcCartneySource.class)
public class BananaConfig extends EasyToEatConfig {

  private final AtomicInteger bananasCount = new AtomicInteger(0);
  private final AtomicInteger nonBananasCount = new AtomicInteger(0);
  private final AtomicInteger exceptionCount = new AtomicInteger(0);

  @Parameter
  @Optional
  @NullSafe
  private HealthyFood healthyFood;

  @OutputResolver(output = FruitMetadataResolver.class)
  public HealthyFood getHealthyFood() {
    return healthyFood;
  }

  public int getBananasCount() {
    return bananasCount.get();
  }

  public int onBanana() {
    return bananasCount.incrementAndGet();
  }

  public int getNonBananasCount() {
    return nonBananasCount.get();
  }

  public int onNotBanana() {
    return nonBananasCount.incrementAndGet();
  }

  public int getExceptionCount() {
    return exceptionCount.get();
  }

  public int onException() {
    return exceptionCount.incrementAndGet();
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.operation.FlowListener;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;

public class FlowListenerBananaOperations {

  @OutputResolver(output = FruitMetadataResolver.class)
  public Fruit getLunch(@Config BananaConfig config, FlowListener listener) {
    final Banana banana = new Banana();
    listener.onSuccess(message -> {
      if (message.getPayload().getValue() instanceof Banana) {
        config.onBanana();
      } else {
        config.onNotBanana();
      }
    });

    listener.onError(exception -> config.onException());
    listener.onComplete(() -> banana.peel());

    return banana;
  }

}

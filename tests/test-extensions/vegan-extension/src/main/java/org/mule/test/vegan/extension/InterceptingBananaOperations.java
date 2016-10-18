/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.runtime.operation.InterceptingCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;

public class InterceptingBananaOperations {

  public InterceptingCallback<Fruit> getLunch(@UseConfig BananaConfig config) {
    return new VeganInterceptor<Fruit>(config) {

      @Override
      public Fruit getResult() {
        return banana;
      }
    };
  }

  public NoGenericsInterceptingCallback getRawLunch(@UseConfig BananaConfig config) {
    return new NoGenericsInterceptingCallback();
  }

  private class NoGenericsInterceptingCallback implements InterceptingCallback {

    @Override
    public Object getResult() throws Exception {
      return new Banana();
    }
  }

  public InterceptingCallback<Result<Fruit, VeganAttributes>> getQualifiedLunch(@UseConfig BananaConfig config) {
    return new VeganInterceptor<Result<Fruit, VeganAttributes>>(config) {

      private final VeganAttributes veganAttributes = new VeganAttributes();

      @Override
      public Result<Fruit, VeganAttributes> getResult() {
        return Result.<Fruit, VeganAttributes>builder().output(banana).attributes(veganAttributes).build();
      }
    };
  }

  private abstract class VeganInterceptor<T> implements InterceptingCallback<T> {

    protected final BananaConfig config;
    protected final Banana banana = new Banana();

    public VeganInterceptor(BananaConfig config) {
      this.config = config;
    }

    @Override
    public void onSuccess(Message resultMessage) {
      if (resultMessage.getPayload().getValue() instanceof Banana) {
        config.onBanana();
      } else {
        config.onNotBanana();
      }
    }

    @Override
    public void onException(Exception exception) {
      config.onException();
    }

    @Override
    public void onComplete() {
      banana.peel();
    }
  }
}

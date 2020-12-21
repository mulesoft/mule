/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Comparator.naturalOrder;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class WatermarkingPetAdoptionSource extends PollingSource<String, Integer> {

  private int pollCounter;
  private static int index = 0;
  private static boolean alreadyWaited = false;
  private static CountDownLatch continueLatch = new CountDownLatch(1);

  public static CountDownLatch beginLatch = new CountDownLatch(1);

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "0")
  protected Integer awaitOnItem;

  @Parameter
  protected List<String> pets;

  @Parameter
  protected List<Integer> watermarks;

  @Parameter
  protected Integer itemsPerPoll;

  @OnSuccess
  public synchronized void onSuccess() {}

  @OnError
  public synchronized void onError() {}

  @OnTerminate
  public synchronized void onTerminate() {}

  @Override
  protected void doStart() throws MuleException {}

  @Override
  protected void doStop() {}

  @Override
  public void poll(PollContext<String, Integer> pollContext) {
    for (int i = 0; i < itemsPerPoll && index < pets.size(); i++, index++) {
      if (index == (awaitOnItem - 1) && !alreadyWaited) {
        try {
          beginLatch.countDown();
          continueLatch.await();
        } catch (InterruptedException e) {
          //Stopping the source will interrupt the latch, so we do nothing and keep pushing items.
        } finally {
          alreadyWaited = true;
        }
      }
      pollContext.setWatermarkComparator(naturalOrder());
      Result<String, Integer> result = Result.<String, Integer>builder().output(pets.get(index)).attributes(pollCounter).build();
      Integer watermarkValue = watermarks.get(index);
      pollContext.accept(item -> {
        item.setResult(result);
        item.setId(result.getOutput().toLowerCase());
        item.setWatermark(watermarkValue);
      });
    }
    pollCounter++;
  }

  @Override
  public void onRejectedItem(Result<String, Integer> result, SourceCallbackContext context) {}

  public static synchronized void resetSource() {
    beginLatch = new CountDownLatch(1);
    continueLatch = new CountDownLatch(1);
    alreadyWaited = false;
    index = 0;
  }

}

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
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.List;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class WatermarkingPetAdoptionSource extends PollingSource<String, Void> {

  public static int STARTED_POLLS;
  private static int index;
  private static int polls;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  protected boolean watermark;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  protected boolean idempotent;

  @Parameter
  protected List<String> pets;

  @Parameter
  protected List<Integer> watermarks;

  @Parameter
  protected Integer itemsPerPoll;

  @Override
  protected void doStart() throws MuleException {
    resetCounters();
    polls = (pets.size() / itemsPerPoll) + 1;
    System.out.println(pets);
  }

  @Override
  protected void doStop() {
    resetCounters();
  }

  @OnSuccess
  public synchronized void onSuccess() {}

  @OnError
  public synchronized void onError() {}

  @OnTerminate
  public synchronized void onTerminate() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    STARTED_POLLS++;
    if (STARTED_POLLS > polls) {
      return;
    }
    for (int i = 0; i < itemsPerPoll && index < pets.size(); i++, index++) {
      pollContext.setWatermarkComparator(naturalOrder());
      Result<String, Void> result = Result.<String, Void>builder().output(pets.get(index)).build();
      Integer watermarkValue = watermarks.get(index);
      pollContext.accept(item -> {
        item.setResult(result);

        if (idempotent) {
          item.setId(result.getOutput().toLowerCase());
        }

        if (watermark) {
          item.setWatermark(watermarkValue);
        }
      });
    }
  }

  @Override
  public void onRejectedItem(Result<String, Void> result, SourceCallbackContext context) {}

  private synchronized void resetCounters() {
    index = polls = STARTED_POLLS = 0;
  }

}

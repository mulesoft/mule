/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Arrays.asList;
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

import java.util.ArrayList;
import java.util.List;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class NumberPetAdoptionSource extends PollingSource<String, Void> {

  public static final List<String> ALL_NUMBERS = asList("50", "3", "20", "51", "90", "52");

  private static int timesPolled;
  private List<String> numbers;


  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  private boolean watermark;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  private boolean idempotent;

  @Override
  protected void doStart() throws MuleException {
    numbers = new ArrayList<>(ALL_NUMBERS);
    resetCounters();
  }

  @Override
  protected void doStop() {
    numbers.clear();
    resetCounters();
  }

  @OnSuccess
  public void onSuccess() {}

  @OnError
  public void onError() {}

  @OnTerminate
  public void onTerminate() {}

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    pollContext.setWatermarkComparator(naturalOrder());

    int amountOfNumbersToPoll = timesPolled == 0 ? 3 : numbers.size();
    numbers.subList(0, amountOfNumbersToPoll).stream()
        .map(p -> Result.<String, Void>builder().output(p).build())
        .forEach(result -> pollContext.accept(item -> {
          item.setResult(result);

          if (idempotent) {
            item.setId(result.getOutput());
          }

          if (watermark) {
            item.setId(result.getOutput());
            item.setWatermark(Integer.valueOf(result.getOutput()));
          }
        }));
    timesPolled++;
  }

  @Override
  public void onRejectedItem(Result<String, Void> result, SourceCallbackContext context) {}

  private void resetCounters() {
    timesPolled = 0;
  }
}

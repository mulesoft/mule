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
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.PollingSource;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.ArrayList;
import java.util.List;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class PetAdoptionSource extends PollingSource<String, Void> {

  public static final List<String> ALL_PETS = asList("Grumpy Cat", "Colonel Meow", "Silvester", "Lil bub", "Macri", "Pappo");
  public static int ADOPTED_PET_COUNT;
  public static int FAILED_ADOPTION_COUNT;
  public static int COMPLETED_POLLS;
  public static int REJECTED_ADOPTIONS;
  protected List<String> pets;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  protected boolean watermark;

  @Parameter
  @org.mule.runtime.extension.api.annotation.param.Optional(defaultValue = "false")
  protected boolean idempotent;

  @Override
  protected void doStart() throws MuleException {
    pets = new ArrayList<>(ALL_PETS);
    resetCounters();
  }

  @Override
  protected void doStop() {
    pets.clear();
    resetCounters();
  }

  @OnSuccess
  public synchronized void onSuccess() {
    ADOPTED_PET_COUNT++;
  }

  @OnError
  public synchronized void onError() {
    FAILED_ADOPTION_COUNT++;
  }

  @OnTerminate
  public synchronized void onTerminate() {
    COMPLETED_POLLS++;
  }

  @Override
  public void poll(PollContext<String, Void> pollContext) {
    pollContext.setWatermarkComparator(naturalOrder());
    pets.stream()
        .map(p -> Result.<String, Void>builder().output(p).build())
        .forEach(result -> pollContext.accept(item -> {
          item.setResult(result);

          if (idempotent) {
            item.setId(result.getOutput());
          }

          if (watermark) {
            item.setWatermark(pets.indexOf(result.getOutput()));
          }
        }));
  }

  @Override
  public void onRejectedItem(Result<String, Void> result, SourceCallbackContext context) {
    REJECTED_ADOPTIONS++;
  }

  private synchronized void resetCounters() {
    ADOPTED_PET_COUNT = FAILED_ADOPTION_COUNT = COMPLETED_POLLS = REJECTED_ADOPTIONS = 0;
  }
}

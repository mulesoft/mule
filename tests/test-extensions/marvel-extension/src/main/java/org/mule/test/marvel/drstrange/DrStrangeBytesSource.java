/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

@Alias("bytes-caster")
@MediaType(TEXT_PLAIN)
public class DrStrangeBytesSource extends Source<InputStream, Void> {

  @Inject
  private SchedulerService schedulerService;
  private Scheduler scheduler;
  private ScheduledFuture sourceCallbackHandleTask;

  @Parameter
  private long castFrequencyInMillis;

  @Parameter
  private String spell;

  @Parameter
  @Optional(defaultValue = "1")
  private int spellSize;

  @Config
  private DrStrange config;

  @Override
  public void onStart(SourceCallback<InputStream, Void> sourceCallback) throws MuleException {
    scheduler = schedulerService.cpuLightScheduler();
    sourceCallbackHandleTask = scheduler.scheduleAtFixedRate(() -> sourceCallback.handle(Result.<InputStream, Void>builder()
        .output(new ByteArrayInputStream(getSpellBytes(spell)))
        .build()), 0, castFrequencyInMillis, MILLISECONDS);
  }

  private byte[] getSpellBytes(String spell) {
    while (spell.length() < spellSize) {
      spell += spell;
    }
    return spell.getBytes();
  }

  @Override
  public void onStop() {
    if (sourceCallbackHandleTask != null) {
      sourceCallbackHandleTask.cancel(false);
    }
    if (scheduler != null) {
      scheduler.stop();
    }
  }
}

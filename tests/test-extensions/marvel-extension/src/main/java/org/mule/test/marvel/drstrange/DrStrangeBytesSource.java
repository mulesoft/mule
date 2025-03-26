/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.ComponentId;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import jakarta.inject.Inject;

import javax.xml.namespace.QName;

@Alias("bytes-caster")
@MediaType(TEXT_PLAIN)
public class DrStrangeBytesSource extends Source<InputStream, Void> {

  @Inject
  private SchedulerService schedulerService;
  private Scheduler scheduler;
  private ScheduledFuture sourceCallbackHandleTask;

  @Inject
  private ConfigurationComponentLocator locator;

  @Parameter
  private long castFrequencyInMillis;

  @Parameter
  private String spell;

  @Parameter
  @Optional(defaultValue = "1")
  private int spellSize;

  @Parameter
  @org.mule.sdk.api.annotation.param.Optional
  @AllowedStereotypes(ReferableOperationStereotypeDefinition.class)
  private String nextOperationReference;

  @Parameter
  @ComponentId
  private String listenerId;

  @Config
  private DrStrange config;


  private ComponentLocation location;
  private QName ANNOTATION_PARAMETERS = new QName("config", "componentParameters");

  @Override
  public void onStart(SourceCallback<InputStream, Void> sourceCallback) throws MuleException {
    if (nextOperationReference != null) {
      Component proc = locator.find(builderFromStringRepresentation(location.getParts().get(0).getPartPath()).addProcessorsPart()
          .addIndexPart(0).build())
          .orElseThrow(() -> new IllegalArgumentException("Missing processor after this source"));

      Map<String, Object> parameters = (Map<String, Object>) proc.getAnnotation(ANNOTATION_PARAMETERS);
      if (!parameters.get("name").equals(nextOperationReference)) {
        throw new IllegalArgumentException(String.format("Next processor %s does not match the expected operation %s",
                                                         parameters.get("name"), nextOperationReference));
      }
    }

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

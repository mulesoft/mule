/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.context;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.context.TransactionProfilingEventContext;
import org.mule.runtime.api.tx.TransactionType;

import java.util.Optional;

/**
 * A {@link TransactionProfilingEventContext} that encapsulates data for the profiling event.
 *
 * @since 4.5
 */
public class DefaultTransactionProfilingEventContext implements TransactionProfilingEventContext {

  private final Optional<ComponentLocation> originalLocation;
  private final ComponentLocation currentLocation;
  private final TransactionType type;
  private final long profilingTimestamp;

  public DefaultTransactionProfilingEventContext(Optional<ComponentLocation> originatingLocation,
                                                 ComponentLocation currentLocation, TransactionType type,
                                                 long profilingTimestamp) {
    this.originalLocation = originatingLocation;
    this.currentLocation = currentLocation;
    this.type = type;
    this.profilingTimestamp = profilingTimestamp;
  }

  @Override
  public long getTriggerTimestamp() {
    return profilingTimestamp;
  }

  @Override
  public TransactionType getType() {
    return type;
  }

  @Override
  public String getTransactionOriginatingLocation() {
    return originalLocation.map(location -> location.getLocation()).orElse(null);
  }

  @Override
  public ComponentLocation getEventOrginatingLocation() {
    return currentLocation;
  }

}

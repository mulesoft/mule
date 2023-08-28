/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.source.scheduler;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.scheduler.SchedulingStrategy;
import org.mule.runtime.extension.api.annotation.Alias;

/**
 *
 * @deprecated Since 4.4, use {@link SchedulingStrategy} instead.
 */
@Deprecated
@Alias("scheduling-strategy")
@NoImplement
public interface Scheduler extends SchedulingStrategy {

}

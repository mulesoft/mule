/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

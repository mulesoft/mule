/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.source.polling;

import static org.mule.runtime.core.config.i18n.CoreMessages.objectIsNull;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.source.polling.schedule.ScheduledPoll;

/**
 * <p>
 * Factory of poll schedules. Every {@link ScheduledPoll} should be created by a {@link ScheduledPollFactory} if the creation
 * process allows post creation hooking.
 * </p>
 * <p>
 *
 * @param <T> is the type of the scheduler job (what is the scheduler going to execute)
 * @since 3.5.0
 */
public abstract class ScheduledPollFactory implements MuleContextAware {

  /**
   * <p>
   * Mule context. Should never be null. In case of being null then the post processing is discarded
   * </p>
   */
  protected MuleContext context;

  /**
   * <p>
   * Creates a scheduler for a job and runs all the registered post processors.
   * </p>
   *
   * @param job The {@link Scheduler} job that has to be executed.
   * @param name The {@link Scheduler} name. This name is the one that is going to be use to register the {@link Scheduler} in the
   *        {@link org.mule.runtime.core.api.registry.MuleRegistry}
   * @return A new instance of a {@link Scheduler}. It must never be null.
   * @throws ScheduledPollCreationException In case after creating and post processing the {@link Scheduler} it is null.
   */
  public final ScheduledPoll create(String name, Runnable job) throws ScheduledPollCreationException {
    ScheduledPoll scheduler = doCreate(name, job);
    checkNull(scheduler);
    return scheduler;
  }

  /**
   * <p>
   * Template method to delegate the creation of the {@link Scheduler}. This method is thought to create an instance of a
   * {@link Scheduler}. It should not Start/Stop it.
   * </p>
   *
   * @param name
   * @param job The Job the {@link org.mule.runtime.core.api.schedule.Scheduler} is going to execute
   * @return The {@link ScheduledPoll} instance
   */
  protected abstract ScheduledPoll doCreate(String name, Runnable job);

  private void checkNull(ScheduledPoll postProcessedScheduler) {
    if (postProcessedScheduler == null) {
      throw new ScheduledPollCreationException(objectIsNull("scheduler").toString());
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.context = context;
  }
}

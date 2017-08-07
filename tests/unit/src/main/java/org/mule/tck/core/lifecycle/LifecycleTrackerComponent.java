/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.lifecycle;

import static org.mockito.Mockito.mock;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.management.stats.ComponentStatistics;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponent extends AbstractLifecycleTracker implements Component {

  public void springInitialize() {
    getTracker().add("springInitialize");
  }

  public void springDestroy() {
    getTracker().add("springDestroy");
  }

  @Override
  public ComponentStatistics getStatistics() {
    return mock(ComponentStatistics.class);
  }

  @Override
  public Event process(Event event) throws MuleException {
    return event;
  }
}

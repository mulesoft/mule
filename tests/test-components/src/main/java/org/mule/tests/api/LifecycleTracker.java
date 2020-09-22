package org.mule.tests.api;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.List;

/**
 * Classes implementing this interface must track its lifecycle phases invocations.
 * The instances must be added to a {@link LifecycleTrackerRegistry} which is added to the registry.
 * For practicality, extend {@link org.mule.tests.internal.BaseLifecycleTracker} instead of implement this interface.
 */
public interface LifecycleTracker extends Initialisable, Disposable, MuleContextAware {

  /**
   * Returns a list of the already invoked phases on this tracker, even if the phase failed.
   */
  List<String> getCalledPhases();
}

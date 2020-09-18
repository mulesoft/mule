package org.mule.tests.api;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.context.MuleContextAware;

import java.util.List;

public interface LifecycleTracker extends Initialisable, Disposable, MuleContextAware {

  List<String> getCalledPhases();
}

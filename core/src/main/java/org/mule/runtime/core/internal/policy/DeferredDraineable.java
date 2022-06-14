package org.mule.runtime.core.internal.policy;

public interface DeferredDraineable<T> {

  Draineable<T> deferredDrain();

}

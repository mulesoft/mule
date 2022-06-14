package org.mule.runtime.core.internal.policy;

import java.util.function.Consumer;

public interface Draineable<T> {

  void drain(Consumer<T> whenDrained);

}

package org.mule.runtime.core.internal.lifecycle.phases;

import java.util.List;

public interface LifecycleObjectSorter {

  void addObject(String name, Object object);

  List<Object> getSortedList();
}

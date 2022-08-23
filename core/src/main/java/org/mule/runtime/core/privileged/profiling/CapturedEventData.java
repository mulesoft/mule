package org.mule.runtime.core.privileged.profiling;

import java.util.Map;

public interface CapturedEventData {

  String getName();

  Map<String, Object> getAttributes();

}

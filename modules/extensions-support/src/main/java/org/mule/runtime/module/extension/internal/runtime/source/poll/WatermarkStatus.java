package org.mule.runtime.module.extension.internal.runtime.source.poll;

public enum WatermarkStatus{
  PASSED, REJECT, ON_HIGH, ON_NEW_HIGH
}
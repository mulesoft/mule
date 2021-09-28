package org.mule.runtime.module.extension.internal.loader.parser.java.source;

import static java.lang.String.format;

import org.mule.runtime.extension.api.runtime.source.BackPressureMode;

/**
 * ADD JDOC
 *
 * @since 4.5.0
 */
public class BackPressureSourceUtils {

  private BackPressureSourceUtils(){}

  public static BackPressureMode fromSdkBackPressureMode(org.mule.sdk.api.runtime.source.BackPressureMode backPressureMode) {
    switch (backPressureMode) {
      case DROP: return BackPressureMode.DROP;
      case FAIL: return BackPressureMode.FAIL;
      case WAIT:return BackPressureMode.WAIT;
    }
    throw new IllegalArgumentException(format("Unexpected back pressure mode %s was given.", backPressureMode.name()));
  }

}

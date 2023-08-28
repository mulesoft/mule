/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.source;

import static java.lang.String.format;

import org.mule.runtime.extension.api.runtime.source.BackPressureMode;
import org.mule.sdk.api.annotation.source.SourceClusterSupport;

/**
 * Utils class for java source related parsing.
 *
 * @since 4.5.0
 */
public class JavaSourceModelParserUtils {

  private JavaSourceModelParserUtils() {}

  public static BackPressureMode fromSdkBackPressureMode(org.mule.sdk.api.runtime.source.BackPressureMode backPressureMode) {
    switch (backPressureMode) {
      case DROP:
        return BackPressureMode.DROP;
      case FAIL:
        return BackPressureMode.FAIL;
      case WAIT:
      default:
        return BackPressureMode.WAIT;
    }
  }

  public static SourceClusterSupport fromLegacySourceClusterSupport(org.mule.runtime.extension.api.annotation.source.SourceClusterSupport legacyClusterSupport) {
    switch (legacyClusterSupport) {
      case NOT_SUPPORTED:
        return SourceClusterSupport.NOT_SUPPORTED;
      case DEFAULT_ALL_NODES:
        return SourceClusterSupport.DEFAULT_ALL_NODES;
      case DEFAULT_PRIMARY_NODE_ONLY:
        return SourceClusterSupport.DEFAULT_PRIMARY_NODE_ONLY;
    }
    throw new IllegalArgumentException(format("Unexpected cluster support mode %s was given.", legacyClusterSupport.name()));
  }
}

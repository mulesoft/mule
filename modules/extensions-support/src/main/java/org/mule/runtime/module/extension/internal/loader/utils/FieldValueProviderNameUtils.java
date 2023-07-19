/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import static java.lang.Math.abs;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.mule.runtime.core.api.util.ClassUtils.hash;

import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;

public class FieldValueProviderNameUtils {

  private static String FIELD_VALUE_PROVIDER_NAME_FORMAT_SUFFIX_SEPARATOR = "[";
  private static String FIELD_VALUE_PROVIDER_NAME_FORMAT = "%s" + FIELD_VALUE_PROVIDER_NAME_FORMAT_SUFFIX_SEPARATOR + "%s]";

  private FieldValueProviderNameUtils() {};

  public static String getFieldValueProviderName(String parameterName, String[] targetSelectors) {
    return format(FIELD_VALUE_PROVIDER_NAME_FORMAT, parameterName, getIdentifierForSelectors(targetSelectors));
  }

  public static String getParameterName(FieldValueProviderModel fieldValueProviderModel) {
    return fieldValueProviderModel.getProviderName()
        .substring(0, fieldValueProviderModel.getProviderName().indexOf(FIELD_VALUE_PROVIDER_NAME_FORMAT_SUFFIX_SEPARATOR));
  }

  private static String getIdentifierForSelectors(String[] targetSelectors) {
    return valueOf(abs(hash(targetSelectors)));
  }

}

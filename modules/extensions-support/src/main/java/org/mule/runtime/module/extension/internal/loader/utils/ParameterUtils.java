/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.utils;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.api.loader.java.type.WithParameters;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterizableTypeWrapper;

import java.util.List;

/**
 * Helper class for introspecting parameter types.
 *
 * @since 4.5
 */
public class ParameterUtils {

  private ParameterUtils() {}

  public static List<FieldElement> getConnectionFields(ParameterizableTypeWrapper parameterizableTypeWrapper) {
    return parameterizableTypeWrapper.getAnnotatedFields(Connection.class, org.mule.sdk.api.annotation.param.Connection.class);
  }

  public static List<FieldElement> getConfigFields(ParameterizableTypeWrapper parameterizableTypeWrapper) {
    return parameterizableTypeWrapper.getAnnotatedFields(Config.class, org.mule.sdk.api.annotation.param.Config.class);
  }

  public static List<FieldElement> getParameterFields(ParameterizableTypeElement parameterizableTypeElement) {
    List<FieldElement> parameterFields = parameterizableTypeElement.getAnnotatedFields(Parameter.class);
    parameterFields.addAll(parameterizableTypeElement.getAnnotatedFields(org.mule.sdk.api.annotation.param.Parameter.class));
    return parameterFields;
  }

  public static List<FieldElement> getParameterGroupFields(ParameterizableTypeElement parameterizableTypeElement) {
    List<FieldElement> parameterFields = parameterizableTypeElement.getAnnotatedFields(ParameterGroup.class);
    parameterFields.addAll(parameterizableTypeElement.getAnnotatedFields(org.mule.sdk.api.annotation.param.ParameterGroup.class));
    return parameterFields;
  }

  public static List<ExtensionParameter> getParameterGroups(WithParameters withParameters) {
    List<ExtensionParameter> parameterGroups = withParameters.getParametersAnnotatedWith(ParameterGroup.class);
    parameterGroups.addAll(withParameters.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.ParameterGroup.class));
    return parameterGroups;
  }
}

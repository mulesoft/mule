/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * A generic contract for any kind of component from which, a list of parameters can be derived
 *
 * @since 4.0
 */
public interface WithParameters {

  /**
   * @return A list of {@link ExtensionParameter} that represents the parameters of the component
   */
  List<ExtensionParameter> getParameters();

  /**
   * @return A list of {@link ExtensionParameter} that represents the parameters of the component that are considered as parameter
   *         groups
   */
  List<ExtensionParameter> getParameterGroups();

  /**
   * @param annotationClass {@link Annotation} to look for parameters annotated with this class
   * @return A list of {@link ExtensionParameter} that are annotated with the given Annotation Class
   */
  List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass);

}

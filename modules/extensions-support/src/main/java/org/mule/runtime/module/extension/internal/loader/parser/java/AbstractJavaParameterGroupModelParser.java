/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterModelParser;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

abstract class AbstractJavaParameterGroupModelParser implements ParameterGroupModelParser {

  protected final ClassTypeLoader typeLoader;
  protected final Function<ParameterModelParser, ParameterModelParser> parameterMutator;

  public AbstractJavaParameterGroupModelParser(ClassTypeLoader typeLoader,
                                               Function<ParameterModelParser, ParameterModelParser> parameterMutator) {
    this.typeLoader = typeLoader;
    this.parameterMutator = parameterMutator;
  }

  @Override
  public final List<ParameterModelParser> getParameterParsers() {
    return doGetParameters()
        .map(p -> {
          ParameterModelParser parser = new JavaParameterModelParser(p, typeLoader);
          if (parameterMutator != null) {
            parser = parameterMutator.apply(parser);
          }
          return parser;
        })
        .collect(toList());
  }

  protected abstract Stream<ExtensionParameter> doGetParameters();
}

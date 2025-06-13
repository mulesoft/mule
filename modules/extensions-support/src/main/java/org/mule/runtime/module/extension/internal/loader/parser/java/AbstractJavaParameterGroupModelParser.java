/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataKeyIdModelParserUtils.parseKeyIdResolverModelParser;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.extension.api.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.extension.api.loader.parser.ParameterModelParser;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Base class for Java based {@link ParameterGroupModelParser} implementations
 *
 * @sinc 4.5.0
 */
abstract class AbstractJavaParameterGroupModelParser implements ParameterGroupModelParser {

  protected final Function<ParameterModelParser, ParameterModelParser> parameterMutator;
  private final ParameterDeclarationContext context;

  public AbstractJavaParameterGroupModelParser(ParameterDeclarationContext context,
                                               Function<ParameterModelParser, ParameterModelParser> parameterMutator) {
    this.context = context;
    this.parameterMutator = parameterMutator;
  }

  @Override
  public final List<ParameterModelParser> getParameterParsers() {
    return doGetParameters()
        .map(p -> {
          ParameterModelParser parser = new JavaParameterModelParser(p, getExclusiveOptionals(), context);
          if (parameterMutator != null) {
            parser = parameterMutator.apply(parser);
          }
          return parser;
        })
        .collect(toList());
  }

  protected abstract Stream<ExtensionParameter> doGetParameters();
}

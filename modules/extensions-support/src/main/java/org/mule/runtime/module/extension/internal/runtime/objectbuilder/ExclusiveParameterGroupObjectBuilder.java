/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.intersection;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.extension.api.declaration.type.TypeUtils;
import org.mule.runtime.extension.api.declaration.type.annotation.ExclusiveOptionalsTypeAnnotation;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;

import com.google.common.base.Joiner;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * {@link DefaultObjectBuilder} extension that validates that the built object complies with
 * the rules specified in {@link org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals}
 *
 * @since 4.0
 */
public final class ExclusiveParameterGroupObjectBuilder<T> extends DefaultObjectBuilder<T> {

  private final ExclusiveOptionalsTypeAnnotation exclusiveOptionalsTypeAnnotation;

  /**
   * Creates a new instance that will build instances of {@code prototypeClass}.
   *
   * @param prototypeClass a {@link Class} which needs to have a public defualt constructor
   */
  public ExclusiveParameterGroupObjectBuilder(Class<T> prototypeClass,
                                              ExclusiveOptionalsTypeAnnotation exclusiveOptionalsTypeAnnotation) {
    super(prototypeClass);
    this.exclusiveOptionalsTypeAnnotation = exclusiveOptionalsTypeAnnotation;
  }

  @Override
  public T build(ValueResolvingContext context) throws MuleException {

    Collection<String> definedExclusiveParameters =
        intersection(exclusiveOptionalsTypeAnnotation.getExclusiveParameterNames(),
                     resolvers.keySet().stream().map(TypeUtils::getAlias).collect(Collectors.toSet()));
    if (definedExclusiveParameters.isEmpty() && exclusiveOptionalsTypeAnnotation.isOneRequired()) {
      throw new ConfigurationException((createStaticMessage(format(
                                                                   "Parameter group of type '%s' requires that one of its optional parameters should be set but all of them are missing. "
                                                                       + "One of the following should be set: [%s]",
                                                                   prototypeClass.getName(),
                                                                   Joiner.on(", ").join(exclusiveOptionalsTypeAnnotation
                                                                       .getExclusiveParameterNames())))));
    } else if (definedExclusiveParameters.size() > 1) {
      throw new ConfigurationException(
                                       createStaticMessage(format("In Parameter group of type '%s', the following parameters cannot be set at the same time: [%s]",
                                                                  prototypeClass.getName(),
                                                                  Joiner.on(", ").join(definedExclusiveParameters))));
    }

    return super.build(context);
  }
}

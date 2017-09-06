/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_PREFIX;
import static org.mule.runtime.module.extension.internal.config.dsl.ExtensionDefinitionParser.CHILD_ELEMENT_KEY_SUFFIX;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.DefaultObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ObjectTypeParametersResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for {@link ObjectFactory} implementation which create extension components.
 * <p>
 * Contains behavior to obtain and manage components parameters.
 *
 * @param <T> the generic type of the instances to be built
 * @since 4.0
 */
public abstract class AbstractExtensionObjectFactory<T> extends AbstractComponentFactory<T>
    implements ObjectTypeParametersResolver {

  protected final MuleContext muleContext;
  protected Map<String, Object> parameters = new HashMap<>();
  protected ParametersResolver parametersResolver;

  public AbstractExtensionObjectFactory(MuleContext muleContext) {
    this.muleContext = muleContext;
    this.parametersResolver = getParametersResolver(muleContext);
  }

  protected ParametersResolver getParametersResolver(MuleContext muleContext) {
    return ParametersResolver.fromValues(parameters, muleContext);
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = normalize(parameters);
    this.parametersResolver = getParametersResolver(muleContext);
  }

  @Override
  public void resolveParameterGroups(ObjectType objectType, DefaultObjectBuilder builder) {
    parametersResolver.resolveParameterGroups(objectType, builder);
  }

  @Override
  public void resolveParameters(ObjectType objectType, DefaultObjectBuilder builder) {
    parametersResolver.resolveParameters(objectType, builder);
  }

  private Map<String, Object> normalize(Map<String, Object> parameters) {
    Map<String, Object> normalized = new HashMap<>();
    parameters.forEach((key, value) -> {
      String normalizedKey = key;

      if (isChildKey(key)) {
        normalizedKey = unwrapChildKey(key);
        normalized.put(normalizedKey, value);
      } else {
        if (!normalized.containsKey(normalizedKey)) {
          normalized.put(normalizedKey, value);
        }
      }
    });

    return normalized;
  }

  private boolean isChildKey(String key) {
    return key.startsWith(CHILD_ELEMENT_KEY_PREFIX) && key.endsWith(CHILD_ELEMENT_KEY_SUFFIX);
  }

  private String unwrapChildKey(String key) {
    return key.replaceAll(CHILD_ELEMENT_KEY_PREFIX, "").replaceAll(CHILD_ELEMENT_KEY_SUFFIX, "");
  }
}

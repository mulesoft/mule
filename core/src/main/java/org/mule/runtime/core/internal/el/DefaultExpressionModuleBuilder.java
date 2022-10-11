/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;


import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultExpressionModuleBuilder implements ExpressionModule.Builder {

  private Map<String, TypedValue<?>> bindings;
  private ModuleNamespace namespace;
  private List<MetadataType> declaredTypes;

  public DefaultExpressionModuleBuilder(ModuleNamespace namespace) {
    this.namespace = namespace;
    this.bindings = new HashMap<>();
    this.declaredTypes = new ArrayList<>();
  }

  @Override
  public ExpressionModule.Builder addBinding(String identifier, TypedValue<?> typedValue) {
    this.bindings.put(identifier, typedValue);
    return this;
  }

  @Override
  public ExpressionModule.Builder addType(MetadataType type) {
    declaredTypes.add(type);
    return this;
  }

  @Override
  public ExpressionModule build() {
    return new DefaultExpressionModule(bindings, namespace, declaredTypes);
  }

  private static class DefaultExpressionModule implements ExpressionModule {

    private Map<String, TypedValue<?>> bindings;
    private ModuleNamespace namespace;
    private List<MetadataType> declaredTypes;

    public DefaultExpressionModule(Map<String, TypedValue<?>> bindings, ModuleNamespace namespace,
                                   List<MetadataType> declaredTypes) {
      this.bindings = bindings;
      this.namespace = namespace;
      this.declaredTypes = declaredTypes;
    }

    @Override
    public Collection<Binding> bindings() {
      return bindings.entrySet().stream().map(entry -> new Binding(entry.getKey(), entry.getValue())).collect(toList());
    }

    @Override
    public Collection<String> identifiers() {
      return bindings.keySet();
    }

    @Override
    public Optional<TypedValue> lookup(String identifier) {
      return Optional.ofNullable(bindings.get(identifier));
    }

    @Override
    public ModuleNamespace namespace() {
      return namespace;
    }

    @Override
    public List<MetadataType> declaredTypes() {
      return declaredTypes;
    }
  }
}

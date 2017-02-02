/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultBindingContextBuilder implements BindingContext.Builder {

  private Map<String, TypedValue> bindings;

  public DefaultBindingContextBuilder() {
    this.bindings = new HashMap<>();
  }

  public DefaultBindingContextBuilder(BindingContext bindingContext) {
    this.bindings = bindingContext.identifiers().stream().collect(toMap(id -> id, id -> bindingContext.lookup(id).get()));
  }

  @Override
  public BindingContext.Builder addBinding(String identifier, TypedValue value) {
    bindings.put(identifier, value);
    return this;
  }

  @Override
  public BindingContext.Builder addAll(BindingContext context) {
    context.identifiers().forEach(id -> bindings.put(id, context.lookup(id).get()));
    return this;
  }

  @Override
  public BindingContext build() {
    return new BindingContextImplementation(bindings);
  }

  private class BindingContextImplementation implements BindingContext {

    private Map<String, TypedValue> bindings;

    private BindingContextImplementation(Map<String, TypedValue> bindings) {
      this.bindings = unmodifiableMap(bindings);
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
      return ofNullable(bindings.get(identifier));
    }
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;

import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


public class DefaultBindingContextBuilder implements BindingContext.Builder {

  private BindingContext parent;

  private TypedValue payloadBinding;
  private TypedValue attributesBinding;
  private Supplier<TypedValue> varsBinding;

  private final Map<String, Supplier<TypedValue>> bindings = new HashMap<>();
  private Collection<ExpressionModule> modules = null;

  public DefaultBindingContextBuilder() {
    this.parent = NULL_BINDING_CONTEXT;
  }

  public DefaultBindingContextBuilder(BindingContext bindingContext) {
    this.parent = bindingContext;

    payloadBinding = bindingContext.lookup(PAYLOAD).orElse(null);
    attributesBinding = bindingContext.lookup(ATTRIBUTES).orElse(null);
    varsBinding = () -> bindingContext.lookup(VARS).orElse(null);
  }

  @Override
  public BindingContext.Builder addBinding(String identifier, TypedValue value) {
    switch (identifier) {
      case PAYLOAD:
        payloadBinding = value;
        break;
      case ATTRIBUTES:
        attributesBinding = value;
        break;
      case VARS:
        varsBinding = () -> value;
        break;
      default:
        bindings.put(identifier, () -> value);
        break;
    }
    return this;
  }

  @Override
  public BindingContext.Builder addBinding(String identifier, Supplier<TypedValue> lazyValue) {
    switch (identifier) {
      case PAYLOAD:
        payloadBinding = lazyValue.get();
        break;
      case ATTRIBUTES:
        attributesBinding = lazyValue.get();
        break;
      case VARS:
        varsBinding = lazyValue;
        break;
      default:
        bindings.put(identifier, lazyValue);
        break;
    }
    return this;
  }

  @Override
  public BindingContext.Builder addAll(BindingContext context) {
    if (parent == NULL_BINDING_CONTEXT) {
      // If no parent, instead of copying the values from the inner maps, just set the parent.
      parent = context;

      payloadBinding = context.lookup(PAYLOAD).orElse(null);
      attributesBinding = context.lookup(ATTRIBUTES).orElse(null);
      varsBinding = () -> context.lookup(VARS).orElse(null);
    } else {
      context.bindings().forEach(binding -> {
        addBinding(binding.identifier(), binding.value());
      });
      if (!context.modules().isEmpty()) {
        modules = context.modules();
      }
    }
    return this;
  }

  @Override
  public BindingContext.Builder addModule(ExpressionModule expressionModule) {
    if (modules == null) {
      modules = new ArrayList<>();
    }
    modules.add(expressionModule);
    return this;
  }

  @Override
  public BindingContext build() {
    return new BindingContextImplementation(parent, unmodifiableMap(bindings),
                                            payloadBinding, attributesBinding, varsBinding,
                                            modules != null ? unmodifiableCollection(modules) : emptyList());
  }

  public static class BindingContextImplementation implements BindingContext {

    private final BindingContext parent;

    private final Optional<TypedValue> payloadBinding;
    private final Optional<TypedValue> attributesBinding;
    private final Supplier<TypedValue> varsBinding;

    private final Map<String, Supplier<TypedValue>> bindings;
    private final Collection<ExpressionModule> modules;

    private BindingContextImplementation(BindingContext parent, Map<String, Supplier<TypedValue>> bindings,
                                         TypedValue payloadBinding, TypedValue attributesBinding,
                                         Supplier<TypedValue> varsBinding,
                                         Collection<ExpressionModule> modules) {
      this.parent = parent;
      this.bindings = bindings;

      this.payloadBinding = payloadBinding != null ? of(payloadBinding) : parent.lookup(PAYLOAD);
      this.attributesBinding = attributesBinding != null ? of(attributesBinding) : parent.lookup(ATTRIBUTES);
      this.varsBinding = varsBinding;

      this.modules = modules;
    }

    @Override
    public Collection<Binding> bindings() {
      final List<Binding> bindingsList = bindings.entrySet().stream()
          .map(entry -> new Binding(entry.getKey(), entry.getValue() != null ? entry.getValue().get() : null))
          .collect(toList());

      bindingsList.addAll(parent.bindings());

      payloadBinding.ifPresent(pb -> bindingsList.add(new Binding(PAYLOAD, payloadBinding.get())));
      attributesBinding.ifPresent(pb -> bindingsList.add(new Binding(ATTRIBUTES, attributesBinding.get())));
      if (varsBinding != null && varsBinding.get() != null) {
        bindingsList.add(new Binding(VARS, varsBinding.get()));
      }

      return bindingsList;
    }

    @Override
    public Collection<String> identifiers() {
      final Set<String> identifiers = new HashSet<>(bindings.keySet());

      identifiers.addAll(parent.identifiers());

      payloadBinding.ifPresent(pb -> identifiers.add(PAYLOAD));
      attributesBinding.ifPresent(pb -> identifiers.add(ATTRIBUTES));
      if (varsBinding != null && varsBinding.get() != null) {
        identifiers.add(VARS);
      }

      return identifiers;
    }

    @Override
    public Optional<TypedValue> lookup(String identifier) {
      switch (identifier) {
        case PAYLOAD:
          return payloadBinding;
        case ATTRIBUTES:
          return attributesBinding;
        case VARS:
          if (varsBinding == null) {
            return parent.lookup(VARS);
          } else {
            return ofNullable(varsBinding.get());
          }
        default:
          final Supplier<TypedValue> supplier = bindings.get(identifier);
          return supplier != null ? ofNullable(supplier.get()) : parent.lookup(identifier);
      }
    }

    @Override
    public Collection<ExpressionModule> modules() {
      List<ExpressionModule> mods = new ArrayList<>();
      mods.addAll(parent.modules());
      mods.addAll(modules);
      return mods;
    }
  }
}

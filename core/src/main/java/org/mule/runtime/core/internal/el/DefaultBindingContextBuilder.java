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
import static org.mule.runtime.api.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.el.BindingContextUtils.VARS;

import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.collection.SmallMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


public class DefaultBindingContextBuilder implements BindingContext.Builder {

  private TypedValue payloadBinding;
  private TypedValue attributesBinding;
  private Optional<Supplier<TypedValue>> varsBinding = Optional.empty();

  private boolean bindingAdded = false;

  private LinkedList<BindingContext> delegates = new LinkedList<>();

  private Map<String, Supplier<TypedValue>> bindings = new SmallMap<>();
  private Collection<ExpressionModule> modules = null;

  public DefaultBindingContextBuilder() {}

  public DefaultBindingContextBuilder(BindingContext bindingContext) {
    this.delegates.add(bindingContext);

    payloadBinding = bindingContext.lookup(PAYLOAD).orElse(null);
    attributesBinding = bindingContext.lookup(ATTRIBUTES).orElse(null);
    varsBinding = bindingContext.lookup(VARS).flatMap(vars -> of(() -> vars));
  }

  @Override
  public BindingContext.Builder addBinding(String identifier, TypedValue value) {
    bindingAdded = true;
    switch (identifier) {
      case PAYLOAD:
        payloadBinding = value;
        break;
      case ATTRIBUTES:
        attributesBinding = value;
        break;
      case VARS:
        varsBinding = of(() -> value);
        break;
      default:
        bindings.put(identifier, () -> value);
        break;
    }
    return this;
  }

  @Override
  public BindingContext.Builder addBinding(String identifier, Supplier<TypedValue> lazyValue) {
    bindingAdded = true;
    switch (identifier) {
      case PAYLOAD:
        payloadBinding = lazyValue.get();
        break;
      case ATTRIBUTES:
        attributesBinding = lazyValue.get();
        break;
      case VARS:
        varsBinding = of(lazyValue);
        break;
      default:
        bindings.put(identifier, lazyValue);
        break;
    }
    return this;
  }

  @Override
  public BindingContext.Builder addAll(BindingContext context) {
    if (bindingAdded) {
      // Because of how the lookup is expected to work, we need to create this context so its bindings are in the correct position
      // in the delegates list.
      delegates.addFirst(new BindingContextImplementation(emptyList(), unmodifiableMap(bindings),
                                                          payloadBinding, attributesBinding, varsBinding,
                                                          modules != null ? unmodifiableCollection(modules) : emptyList()));
      payloadBinding = null;
      attributesBinding = null;
      varsBinding = Optional.empty();
      bindings = new HashMap<>();
      modules = null;
      bindingAdded = false;
    }

    payloadBinding = context.lookup(PAYLOAD).orElse(payloadBinding);
    attributesBinding = context.lookup(ATTRIBUTES).orElse(attributesBinding);
    context.lookup(VARS).ifPresent(vars -> {
      varsBinding = of(() -> vars);
    });

    delegates.addFirst(context);
    return this;
  }

  @Override
  public BindingContext.Builder addModule(ExpressionModule expressionModule) {
    bindingAdded = true;
    if (modules == null) {
      modules = new ArrayList<>();
    }
    modules.add(expressionModule);
    return this;
  }

  @Override
  public BindingContext build() {
    return new BindingContextImplementation(new ArrayList<>(delegates), unmodifiableMap(bindings),
                                            payloadBinding, attributesBinding, varsBinding,
                                            modules != null ? unmodifiableCollection(modules) : emptyList());
  }

  public BindingContext flattenAndBuild() {
    BindingContext original = build();

    Map<String, Supplier<TypedValue>> flattenedBindings = new HashMap<>();
    for (Binding binding : original.bindings()) {
      if (!(flattenedBindings.containsKey(binding.identifier())
          || PAYLOAD.equals(binding.identifier())
          || ATTRIBUTES.equals(binding.identifier())
          || VARS.equals(binding.identifier()))) {
        flattenedBindings.put(binding.identifier(), () -> binding.value());
      }
    }

    return new BindingContextImplementation(emptyList(), flattenedBindings,
                                            original.lookup(PAYLOAD).orElse(null),
                                            original.lookup(ATTRIBUTES).orElse(null),
                                            of(() -> original.lookup(VARS).orElse(null)),
                                            original.modules());
  }


  public static class BindingContextImplementation implements BindingContext {

    private final List<BindingContext> delegates;

    private final Optional<TypedValue> payloadBinding;
    private final Optional<TypedValue> attributesBinding;
    private final Supplier<TypedValue> varsBinding;

    private final Map<String, Supplier<TypedValue>> bindings;
    private final Collection<ExpressionModule> modules;

    private BindingContextImplementation(List<BindingContext> delegates, Map<String, Supplier<TypedValue>> bindings,
                                         TypedValue payloadBinding, TypedValue attributesBinding,
                                         Optional<Supplier<TypedValue>> varsBinding,
                                         Collection<ExpressionModule> modules) {
      this.delegates = delegates;
      this.bindings = bindings;

      this.payloadBinding = ofNullable(payloadBinding);
      this.attributesBinding = ofNullable(attributesBinding);
      this.varsBinding = varsBinding.orElse(() -> null);

      this.modules = modules;
    }

    private Optional<TypedValue> lookUpInDelegates(String identifier) {
      for (BindingContext bindingContext : this.delegates) {
        Optional<TypedValue> result = bindingContext.lookup(identifier);
        if (result.isPresent()) {
          return result;
        }
      }
      return Optional.empty();
    }

    @Override
    public Collection<Binding> bindings() {
      Map<String, Binding> bindingsMap = new HashMap<>();

      for (Entry<String, Supplier<TypedValue>> entry : bindings.entrySet()) {
        bindingsMap.put(entry.getKey(), new Binding(entry.getKey(), entry.getValue() != null ? entry.getValue().get() : null));
      }

      payloadBinding.ifPresent(pb -> bindingsMap.put(PAYLOAD, new Binding(PAYLOAD, payloadBinding.get())));
      attributesBinding.ifPresent(pb -> bindingsMap.put(ATTRIBUTES, new Binding(ATTRIBUTES, attributesBinding.get())));
      if (varsBinding.get() != null) {
        bindingsMap.put(VARS, new Binding(VARS, varsBinding.get()));
      }

      for (BindingContext bindingContext : delegates) {
        for (Binding binding : bindingContext.bindings()) {
          bindingsMap.putIfAbsent(binding.identifier(), binding);
        }
      }

      return bindingsMap.values();
    }

    @Override
    public Collection<String> identifiers() {
      final Set<String> identifiers = new HashSet<>(bindings.keySet());

      payloadBinding.ifPresent(pb -> identifiers.add(PAYLOAD));
      attributesBinding.ifPresent(pb -> identifiers.add(ATTRIBUTES));
      if (varsBinding.get() != null) {
        identifiers.add(VARS);
      }

      for (BindingContext bindingContext : delegates) {
        identifiers.addAll(bindingContext.identifiers());
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
          return ofNullable(varsBinding.get());
        default:
          final Supplier<TypedValue> supplier = bindings.get(identifier);
          return supplier != null ? ofNullable(supplier.get()) : lookUpInDelegates(identifier);
      }
    }

    @Override
    public Collection<ExpressionModule> modules() {
      List<ExpressionModule> mods = new ArrayList<>();
      mods.addAll(modules);
      for (BindingContext bindingContext : delegates) {
        mods.addAll(bindingContext.modules());
      }
      return mods;
    }
  }
}

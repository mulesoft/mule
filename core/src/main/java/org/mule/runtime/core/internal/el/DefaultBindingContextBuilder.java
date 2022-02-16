/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.el.BindingContextUtils.ATTRIBUTES;
import static org.mule.runtime.api.el.BindingContextUtils.AUTHENTICATION;
import static org.mule.runtime.api.el.BindingContextUtils.CORRELATION_ID;
import static org.mule.runtime.api.el.BindingContextUtils.ERROR;
import static org.mule.runtime.api.el.BindingContextUtils.ITEM_SEQUENCE_INFO;
import static org.mule.runtime.api.el.BindingContextUtils.PARAMS;
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
  private Optional<Supplier<TypedValue>> varsBinding = empty();
  private Optional<Supplier<TypedValue>> paramsBinding = empty();
  private TypedValue errorBinding;
  private TypedValue correlationIdBinding;
  private TypedValue authenticationBinding;
  private TypedValue itemSequenceInfoBinding;

  private boolean bindingAdded = false;

  private LinkedList<BindingContext> delegates = null;

  private Map<String, Supplier<TypedValue>> bindings = null;
  private Collection<ExpressionModule> modules = null;

  public DefaultBindingContextBuilder() {}

  public DefaultBindingContextBuilder(BindingContext bindingContext) {
    this.delegates = new LinkedList<>();
    this.delegates.add(bindingContext);

    payloadBinding = bindingContext.lookup(PAYLOAD).orElse(null);
    attributesBinding = bindingContext.lookup(ATTRIBUTES).orElse(null);
    varsBinding = bindingContext.lookup(VARS).flatMap(vars -> of(() -> vars));
    paramsBinding = bindingContext.lookup(PARAMS).flatMap(params -> of(() -> params));
    errorBinding = bindingContext.lookup(ERROR).orElse(null);
    correlationIdBinding = bindingContext.lookup(CORRELATION_ID).orElse(null);
    authenticationBinding = bindingContext.lookup(AUTHENTICATION).orElse(null);
    itemSequenceInfoBinding = bindingContext.lookup(ITEM_SEQUENCE_INFO).orElse(null);
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
      case PARAMS:
        paramsBinding = of(() -> value);
        break;
      case ERROR:
        errorBinding = value;
        break;
      case CORRELATION_ID:
        correlationIdBinding = value;
        break;
      case AUTHENTICATION:
        authenticationBinding = value;
        break;
      case ITEM_SEQUENCE_INFO:
        itemSequenceInfoBinding = value;
        break;
      default:
        if (bindings == null) {
          bindings = new SmallMap<>();
        }
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
      case PARAMS:
        paramsBinding = of(lazyValue);
        break;
      case ERROR:
        errorBinding = lazyValue.get();
        break;
      case CORRELATION_ID:
        correlationIdBinding = lazyValue.get();
        break;
      case AUTHENTICATION:
        authenticationBinding = lazyValue.get();
        break;
      case ITEM_SEQUENCE_INFO:
        itemSequenceInfoBinding = lazyValue.get();
        break;
      default:
        if (bindings == null) {
          bindings = new SmallMap<>();
        }
        bindings.put(identifier, lazyValue);
        break;
    }
    return this;
  }

  @Override
  public BindingContext.Builder addAll(BindingContext context) {
    if (delegates == null) {
      delegates = new LinkedList<>();
    }

    if (bindingAdded) {
      // Because of how the lookup is expected to work, we need to create this context so its bindings are in the correct position
      // in the delegates list.
      delegates.addFirst(new BindingContextImplementation(
                                                          emptyList(),
                                                          bindings == null ? emptyMap() : unmodifiableMap(bindings),
                                                          payloadBinding,
                                                          attributesBinding,
                                                          varsBinding,
                                                          paramsBinding,
                                                          errorBinding,
                                                          correlationIdBinding,
                                                          authenticationBinding,
                                                          itemSequenceInfoBinding,
                                                          modules != null ? unmodifiableCollection(modules) : emptyList()));

      payloadBinding = null;
      attributesBinding = null;
      varsBinding = empty();
      paramsBinding = empty();
      errorBinding = null;
      correlationIdBinding = null;
      authenticationBinding = null;
      itemSequenceInfoBinding = null;
      bindings = null;
      modules = null;
      bindingAdded = false;
    }

    payloadBinding = context.lookup(PAYLOAD).orElse(payloadBinding);
    attributesBinding = context.lookup(ATTRIBUTES).orElse(attributesBinding);
    context.lookup(VARS).ifPresent(vars -> varsBinding = of(() -> vars));
    context.lookup(PARAMS).ifPresent(params -> paramsBinding = of(() -> params));
    errorBinding = context.lookup(ERROR).orElse(errorBinding);
    correlationIdBinding = context.lookup(CORRELATION_ID).orElse(correlationIdBinding);
    authenticationBinding = context.lookup(AUTHENTICATION).orElse(authenticationBinding);
    itemSequenceInfoBinding = context.lookup(ITEM_SEQUENCE_INFO).orElse(itemSequenceInfoBinding);

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
    return new BindingContextImplementation(
                                            delegates == null || delegates.isEmpty() ? emptyList() : new ArrayList<>(delegates),
                                            bindings == null ? emptyMap() : unmodifiableMap(bindings),
                                            payloadBinding,
                                            attributesBinding,
                                            varsBinding,
                                            paramsBinding,
                                            errorBinding,
                                            correlationIdBinding,
                                            authenticationBinding,
                                            itemSequenceInfoBinding,
                                            modules != null ? unmodifiableCollection(modules) : emptyList());
  }

  public BindingContext flattenAndBuild() {
    BindingContext original = build();

    Map<String, Supplier<TypedValue>> flattenedBindings = new HashMap<>();
    for (Binding binding : original.bindings()) {
      if (!(flattenedBindings.containsKey(binding.identifier())
          || PAYLOAD.equals(binding.identifier())
          || ATTRIBUTES.equals(binding.identifier())
          || VARS.equals(binding.identifier())
          || PARAMS.equals(binding.identifier())
          || ERROR.equals(binding.identifier())
          || CORRELATION_ID.equals(binding.identifier())
          || AUTHENTICATION.equals(binding.identifier())
          || ITEM_SEQUENCE_INFO.equals(binding.identifier()))) {
        flattenedBindings.put(binding.identifier(), () -> binding.value());
      }
    }

    return new BindingContextImplementation(emptyList(), flattenedBindings,
                                            original.lookup(PAYLOAD).orElse(null),
                                            original.lookup(ATTRIBUTES).orElse(null),
                                            of(() -> original.lookup(VARS).orElse(null)),
                                            of(() -> original.lookup(PARAMS).orElse(null)),
                                            original.lookup(ERROR).orElse(null),
                                            original.lookup(CORRELATION_ID).orElse(null),
                                            original.lookup(AUTHENTICATION).orElse(null),
                                            original.lookup(ITEM_SEQUENCE_INFO).orElse(null),
                                            original.modules());
  }


  public static class BindingContextImplementation implements BindingContext {

    private final List<BindingContext> delegates;

    private final Optional<TypedValue> payloadBinding;
    private final Optional<TypedValue> attributesBinding;
    private final Supplier<TypedValue> varsBinding;
    private final Supplier<TypedValue> paramsBinding;
    private final Optional<TypedValue> errorBinding;
    private final Optional<TypedValue> correlationIdBinding;
    private final Optional<TypedValue> authenticationBinding;
    private final Optional<TypedValue> itemSequenceInfoBinding;

    private final Map<String, Supplier<TypedValue>> bindings;
    private final Collection<ExpressionModule> modules;

    private BindingContextImplementation(List<BindingContext> delegates,
                                         Map<String, Supplier<TypedValue>> bindings,
                                         TypedValue payloadBinding, TypedValue attributesBinding,
                                         Optional<Supplier<TypedValue>> varsBinding,
                                         Optional<Supplier<TypedValue>> paramsBinding,
                                         TypedValue errorBinding, TypedValue correlationIdBinding,
                                         TypedValue authenticationBinding,
                                         TypedValue itemSequenceInfoBinding,
                                         Collection<ExpressionModule> modules) {
      this.delegates = delegates;
      this.bindings = bindings;

      this.payloadBinding = ofNullable(payloadBinding);
      this.attributesBinding = ofNullable(attributesBinding);
      this.varsBinding = varsBinding.orElse(() -> null);
      this.paramsBinding = paramsBinding.orElse(() -> null);
      this.errorBinding = ofNullable(errorBinding);
      this.correlationIdBinding = ofNullable(correlationIdBinding);
      this.authenticationBinding = ofNullable(authenticationBinding);
      this.itemSequenceInfoBinding = ofNullable(itemSequenceInfoBinding);

      this.modules = modules;
    }

    private Optional<TypedValue> lookUpInDelegates(String identifier) {
      for (BindingContext bindingContext : this.delegates) {
        Optional<TypedValue> result = bindingContext.lookup(identifier);
        if (result.isPresent()) {
          return result;
        }
      }
      return empty();
    }

    @Override
    public Collection<Binding> bindings() {
      Map<String, Binding> bindingsMap = new HashMap<>();

      for (Entry<String, Supplier<TypedValue>> entry : bindings.entrySet()) {
        bindingsMap.put(entry.getKey(), new Binding(entry.getKey(), entry.getValue() != null ? entry.getValue().get() : null));
      }

      payloadBinding.ifPresent(pb -> bindingsMap.put(PAYLOAD, new Binding(PAYLOAD, pb)));
      attributesBinding.ifPresent(ab -> bindingsMap.put(ATTRIBUTES, new Binding(ATTRIBUTES, ab)));

      TypedValue vars = varsBinding.get();
      if (vars != null) {
        bindingsMap.put(VARS, new Binding(VARS, vars));
      }

      TypedValue params = paramsBinding.get();
      if (params != null) {
        bindingsMap.put(PARAMS, new Binding(PARAMS, params));
      }

      errorBinding.ifPresent(eb -> bindingsMap.put(ERROR, new Binding(ERROR, eb)));
      correlationIdBinding.ifPresent(cidb -> bindingsMap.put(CORRELATION_ID, new Binding(CORRELATION_ID, cidb)));
      authenticationBinding.ifPresent(ab -> bindingsMap.put(AUTHENTICATION, new Binding(AUTHENTICATION, ab)));
      itemSequenceInfoBinding.ifPresent(isib -> bindingsMap.put(ITEM_SEQUENCE_INFO, new Binding(ITEM_SEQUENCE_INFO, isib)));

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

      if (paramsBinding.get() != null) {
        identifiers.add(PARAMS);
      }

      errorBinding.ifPresent(pb -> identifiers.add(ERROR));
      correlationIdBinding.ifPresent(pb -> identifiers.add(CORRELATION_ID));
      authenticationBinding.ifPresent(pb -> identifiers.add(AUTHENTICATION));
      itemSequenceInfoBinding.ifPresent(pb -> identifiers.add(ITEM_SEQUENCE_INFO));

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
        case PARAMS:
          return ofNullable(paramsBinding.get());
        case ERROR:
          return errorBinding;
        case CORRELATION_ID:
          return correlationIdBinding;
        case AUTHENTICATION:
          return authenticationBinding;
        case ITEM_SEQUENCE_INFO:
          return itemSequenceInfoBinding;
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

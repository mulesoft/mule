/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.el.Binding;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Composite version of the {@link ExpressionModule} interface. The privileged extensions can add an {@link ExpressionModule},
 * wrapped into a binding context, to the registry. The {@code ExtensionActivator} class adds an {@link ExpressionModule}, wrapped
 * into a binding context, to the registry. That makes possible that two instances of {@link ExpressionModule} with the same
 * namespace be present in the global binding context. If that's the case, the expression language won't be able to find the
 * bindings provided by one of them. This class is used to merge two instances of {@link ExpressionModule}, and it's intended to
 * be used in the default {@link BindingContext} implementation.
 *
 * @since 4.6.0
 */
class CompositeExpressionModule implements ExpressionModule {

  private final List<ExpressionModule> expressionModules;
  private final Collection<Binding> bindings;
  private final Collection<String> identifiers;
  private final ModuleNamespace namespace;
  private final List<MetadataType> types;

  public CompositeExpressionModule(ExpressionModule... modules) {
    expressionModules = asList(modules);
    bindings = expressionModules.stream().flatMap(em -> em.bindings().stream()).collect(toList());
    identifiers = expressionModules.stream().flatMap(em -> em.identifiers().stream()).collect(toList());
    namespace = getAndCheckUniqueNamespace(expressionModules);
    types = expressionModules.stream().flatMap(em -> em.declaredTypes().stream()).collect(toList());
  }

  @Override
  public Collection<Binding> bindings() {
    return bindings;
  }

  @Override
  public Collection<String> identifiers() {
    return identifiers;
  }

  @Override
  public ModuleNamespace namespace() {
    return namespace;
  }

  @Override
  public List<MetadataType> declaredTypes() {
    return types;
  }

  @Override
  public Optional<TypedValue> lookup(String identifier) {
    for (ExpressionModule expressionModule : expressionModules) {
      Optional<TypedValue> found = expressionModule.lookup(identifier);
      if (found.isPresent()) {
        return found;
      }
    }
    return empty();
  }

  private ModuleNamespace getAndCheckUniqueNamespace(List<ExpressionModule> modules) {
    Set<ModuleNamespace> namespacesSet = modules.stream().map(ExpressionModule::namespace).collect(toSet());
    if (namespacesSet.size() != 1) {
      throw new IllegalArgumentException(format("Expected unique namespace but found multiple %s", namespacesSet));
    }
    return namespacesSet.iterator().next();
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.module.extension.internal.util.IdempotentExtensionWalker;

import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.util.HashSet;
import java.util.Set;

/**
 * Validates that there's no name clashing among the extension's connection providers
 *
 * @since 4.0
 */
public class ConnectionProviderNameModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    Multiset<String> names = HashMultiset.create();
    Set<ConnectionProviderModel> models = new HashSet<>();
    new IdempotentExtensionWalker() {

      @Override
      public void onConnectionProvider(ConnectionProviderModel model) {
        models.add(model);
        names.add(model.getName());
      }
    }.walk(model);

    Set<ConnectionProviderModel> repeatedNameModels =
        models.stream().filter(cp -> names.count(cp.getName()) > 1).collect(toSet());

    if (!repeatedNameModels.isEmpty()) {
      throw new IllegalModelDefinitionException(format("Extension '%s' defines %d connection providers with repeated names. "
          + "Offending names are: [%s]", model.getName(), repeatedNameModels
              .size(), Joiner.on(", ").join(repeatedNameModels.stream().map(Named::getName).collect(toSet()))));
    }
  }
}

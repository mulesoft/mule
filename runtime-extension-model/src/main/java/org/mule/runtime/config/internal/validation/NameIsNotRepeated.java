/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.currentElemement;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.equalsComponentId;
import static org.mule.runtime.ast.api.util.ComponentAstPredicatesFactory.topLevelElement;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.ValidationResultItem.create;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableSet;

/**
 * 'name' attribute in top-level elements are unique.
 */
public class NameIsNotRepeated implements Validation {

  private static final String MULE_ROOT_ELEMENT = "mule";

  public static final String TEST_NAMESPACE = "test";

  private static ImmutableSet<ComponentIdentifier> ignoredNameValidationComponentList =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("alias").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("password-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-security-provider")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("custom-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT)
              .name("secret-key-encryption-strategy")
              .build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("import").build())
          .add(builder().namespace(MULE_ROOT_ELEMENT).name("security-manager").build())
          // TODO MULE-18366 Remove these entries from test namespace
          .add(builder().namespace(TEST_NAMESPACE).name("queue").build())
          .add(builder().namespace(TEST_NAMESPACE).name("invocation-counter").build())
          .build();


  @Override
  public String getName() {
    return "Names are not repeated";
  }

  @Override
  public String getDescription() {
    return "'name' attribute in top-level elements are unique.";
  }

  @Override
  public Level getLevel() {
    return ERROR;
  }

  @Override
  public Predicate<List<ComponentAst>> applicable() {
    return topLevelElement()
        .and(currentElemement(component -> component.getComponentId().isPresent()))
        .and(currentElemement(component -> !ignoredNameValidationComponentList.contains(component.getIdentifier())))
        .and(currentElemement(component -> !component.getModel(HasStereotypeModel.class)
            .map(sm -> sm.getStereotype().isAssignableTo(APP_CONFIG))
            .orElse(false)));
  }

  @Override
  public Optional<ValidationResultItem> validate(ComponentAst component, ArtifactAst artifact) {
    final List<ComponentAst> repeated = artifact.topLevelComponentsStream()
        .filter(comp -> !comp.equals(component))
        .filter(equalsComponentId(component.getComponentId().get()))
        .collect(toList());

    if (repeated.isEmpty()) {
      return empty();
    }

    final List<ComponentAst> allRepeated = new ArrayList<>();
    allRepeated.add(component);
    allRepeated.addAll(repeated);

    return of(create(allRepeated, this,
                     "Two (or more) configuration elements have been defined with the same global name. Global name '"
                         + component.getComponentId().get() + "' must be unique."));
  }

}

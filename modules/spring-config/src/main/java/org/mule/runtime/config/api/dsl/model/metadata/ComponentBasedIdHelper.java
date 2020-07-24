/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Comparator.comparing;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.AttributeFieldType;
import org.mule.metadata.api.model.AttributeKeyType;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.FunctionType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.LocalDateTimeType;
import org.mule.metadata.api.model.LocalTimeType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NothingType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectKeyType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.PeriodType;
import org.mule.metadata.api.model.RegexType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.TimeType;
import org.mule.metadata.api.model.TimeZoneType;
import org.mule.metadata.api.model.TupleType;
import org.mule.metadata.api.model.TypeParameterType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.model.VoidType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

public class ComponentBasedIdHelper {

  static String getSourceElementName(ComponentAst elementModel) {
    return elementModel.getIdentifier().toString()
        + getModelNameAst(elementModel).orElse(elementModel.getComponentId().map(n -> "[" + n + "]").orElse(""));
  }

  static Optional<String> getModelNameAst(ComponentAst component) {
    final Optional<NamedObject> namedObjectModel = component.getModel(NamedObject.class);
    if (namedObjectModel.isPresent()) {
      return namedObjectModel.map(n -> n.getName());
    }

    final Optional<Typed> typedObjectModel = component.getModel(Typed.class);
    if (typedObjectModel.isPresent()) {
      return typedObjectModel.map(t -> ExtensionMetadataTypeUtils.getId(t.getType()).toString());
    }

    return empty();
  }

  static String sourceElementNameFromSimpleValue(ComponentAst element) {
    return getModelNameAst(element)
        .map(modelName -> element.getIdentifier().getNamespace() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().toString());
  }

  static String sourceElementNameFromSimpleValue(ComponentAst owner, ComponentParameterAst element) {
    return owner.getIdentifier().getNamespace() + ":" + element.getModel().getName();
  }

  static Optional<String> resolveConfigName(ComponentAst elementModel) {
    // TODO MULE-18327 Migrate to Stereotypes when config-ref is part of model
    // There seems to be something missing in the mock model from the unit tests and this fails.
    // return MuleAstUtils.parameterOfType(elementModel, MuleStereotypes.CONFIG)
    // .map(p -> p.getValue().reduce(identity(), v -> v.toString()));
    return elementModel.getRawParameterValue(CONFIG_ATTRIBUTE_NAME);
  }

  public static int computeHashFor(ComponentParameterAst componentParameterAst) {
    return ParameterVisitorFunctions.computeHashFor(componentParameterAst);
  }

  private static class ParameterVisitorFunctions {

    private static int computeHashFor(ComponentParameterAst parameter) {
      return hash(new ParameterVisitorFunctions(parameter).hashBuilder.toString());
    }

    private StringBuilder hashBuilder = new StringBuilder();
    private final Function<String, Void> leftFunction = this::hashForLeft;
    private final Function<Object, Void> rightFunction = this::hashForRight;

    private ParameterVisitorFunctions(ComponentParameterAst startingParameter) {
      startingParameter.getValue().reduce(leftFunction, rightFunction);
    }

    private Void hashForLeft(String s) {
      hashBuilder.append(s);
      return null;
    }

    private Void hashForRight(Object o) {
      if (o instanceof ComponentAst) {
        final ComponentAst c = (ComponentAst) o;
        c.getParameters().stream().sorted(comparing(p -> p.getModel().getName())).forEach(p -> {
          hashBuilder.append(p.getModel().getName());
          if (p.getModel().getType() instanceof ArrayType) {
            hashForList((Collection<ComponentAst>) p.getValue().getRight());
          } else {
            p.getValue().reduce(leftFunction, rightFunction);
          }
        });
      } else {
        hashBuilder.append(o);
      }
      return null;
    }

    private void hashForList(Collection<ComponentAst> collection) {
      collection.forEach(c -> c.getParameter("value").getValue().reduce(leftFunction, rightFunction));
    }

  }


}

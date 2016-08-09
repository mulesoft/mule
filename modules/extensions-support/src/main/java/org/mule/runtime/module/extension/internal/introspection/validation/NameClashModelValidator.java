/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.api.util.Reference;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.Described;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.Named;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.introspection.connection.HasConnectionProviderModels;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterizedModel;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;

import com.google.common.base.Joiner;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Validates names clashes in the model by comparing:
 * <ul>
 * <li>The {@link Named#getName()} value of all the {@link ConfigurationModel}, {@link OperationModel} and
 * {@link ConnectionProviderModel}</li>
 * <li>Makes sure that there no two {@link ParameterModel}s with the same name but different types, for those which represent an
 * object</li>
 * <li>Makes sure that no {@link ConfigurationModel}, {@link OperationModel} or {@link ConnectionProviderModel} have parameters
 * with repeated name</li>
 * </ul>
 *
 * @since 4.0
 */
public final class NameClashModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    new ValidationDelegate(model).validate(model);
  }

  private class ValidationDelegate {

    private final ExtensionModel extensionModel;
    private final Set<DescribedReference<Named>> namedObjects = new HashSet<>();
    private final Multimap<String, TopLevelParameter> topLevelParameters = LinkedListMultimap.create();

    public ValidationDelegate(ExtensionModel extensionModel) {
      this.extensionModel = extensionModel;
    }

    private void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
      new ExtensionWalker() {

        @Override
        public void onConfiguration(ConfigurationModel model) {
          defaultValidation(model);
        }

        @Override
        public void onOperation(HasOperationModels owner, OperationModel model) {
          validateOperation(model);
          registerNamedObject(model);
        }

        @Override
        public void onConnectionProvider(HasConnectionProviderModels owner, ConnectionProviderModel model) {
          defaultValidation(model);
        }

        @Override
        public void onSource(HasSourceModels owner, SourceModel model) {
          defaultValidation(model);
        }

        @Override
        public void onParameter(ParameterizedModel owner, ParameterModel model) {
          validateTopLevelParameter(model, owner);
        }

        private void defaultValidation(ParameterizedModel model) {
          validateParameterNames(model);
          registerNamedObject(model);
        }

        private void registerNamedObject(Named named) {
          namedObjects.add(new DescribedReference<>(named));
        }
      }.walk(extensionModel);

      validateNameClashes(namedObjects, topLevelParameters.values(),
                          topLevelParameters.values().stream().map(TypedTopLevelParameter::new).collect(toSet()));
    }

    private void validateOperation(OperationModel operation) {
      validateParameterNames(operation);
      // Check clash between each operation and its parameters type
      operation.getParameterModels().stream()
          .forEach(parameterModel -> validateClash(operation.getName(), getType(parameterModel.getType()).getName(), "operation",
                                                   "argument"));
    }

    private void validateParameterNames(ParameterizedModel model) {
      Set<String> repeatedParameters = collectRepeatedNames(model.getParameterModels());
      if (!repeatedParameters.isEmpty()) {
        throw new IllegalModelDefinitionException(format("Extension '%s' defines the %s '%s' which has parameters "
            + "with repeated names. Offending parameters are: [%s]", extensionModel.getName(), model.getClass().getSimpleName(),
                                                         model.getName(), Joiner.on(",").join(repeatedParameters)));
      }
    }

    private void validateTopLevelParameter(ParameterModel parameter, ParameterizedModel owner) {
      if (!(parameter.getType() instanceof ObjectType)) {
        return;
      }

      final Class<?> parameterType = getType(parameter.getType());
      final String ownerName = owner.getName();
      final String ownerType = owner.getClass().getSimpleName();

      Collection<TopLevelParameter> foundParameters = topLevelParameters.get(parameter.getName());
      if (CollectionUtils.isEmpty(foundParameters)) {
        topLevelParameters.put(parameter.getName(), new TopLevelParameter(parameter, ownerName, ownerType));
      } else {
        Optional<TopLevelParameter> repeated =
            foundParameters.stream().filter(topLevelParameter -> !topLevelParameter.type.equals(parameterType)).findFirst();

        if (repeated.isPresent()) {
          TopLevelParameter tp = repeated.get();
          throw new IllegalModelDefinitionException(format("Extension '%s' defines a %s of name '%s' which contains a parameter of complex type '%s'. However, "
              + "%s of name '%s' defines a parameter of the same name but type '%s'. Complex parameter of different types cannot have the same name.",
                                                           extensionModel.getName(), ownerType, ownerName, parameterType,
                                                           tp.ownerType, tp.owner, tp.type.getName()));
        }
      }
    }

    private Set<String> collectRepeatedNames(List<? extends Named> namedObject) {
      Set<String> names = new HashSet<>();
      Set<String> repeatedNames =
          namedObject.stream().filter(parameter -> !names.add(parameter.getName())).map(Named::getName).collect(toSet());

      return repeatedNames;
    }

    private void validateNameClashes(Collection<? extends Named>... collections) {
      Multimap<String, Named> names = LinkedListMultimap.create();
      stream(collections).flatMap(Collection::stream).forEach(named -> names.put(named.getName(), named));

      names.asMap().entrySet().forEach(entry -> {
        List<Named> values = (List<Named>) entry.getValue();
        if (values.size() > 1) {
          Set<String> offendingTypes = values.stream().map(Named::getName).collect(toSet());
          StringBuilder errorMessage =
              new StringBuilder(format("Extension '%s' contains %d ", extensionModel.getName(), values.size()));

          final int top = offendingTypes.size() - 1;
          int i = 0;
          for (String offender : offendingTypes) {
            errorMessage.append(offender);

            if (i + 1 == top) {
              errorMessage.append(" and ");
            } else if (i != top) {
              errorMessage.append(", ");
            }

            i++;
          }

          errorMessage.append(format(" which name is '%s'. Names should be unique", entry.getKey()));
          throw new IllegalModelDefinitionException(errorMessage.toString());
        }
      });
    }

    private void validateClash(String existingNamingModel, String newNamingModel, String typeOfExistingNamingModel,
                               String typeOfNewNamingModel) {
      if (StringUtils.equalsIgnoreCase(existingNamingModel, newNamingModel)) {
        throw new IllegalModelDefinitionException(format("Extension '%s' has a %s named '%s' with an %s type named equally.",
                                                         extensionModel.getName(), typeOfExistingNamingModel, existingNamingModel,
                                                         typeOfNewNamingModel));
      }
    }
  }


  private class TopLevelParameter implements Named, Described {

    protected final ParameterModel parameterModel;
    protected final String owner;
    protected final String ownerType;
    protected final Class<?> type;

    private TopLevelParameter(ParameterModel parameterModel, String owner, String ownerType) {
      this.parameterModel = parameterModel;
      this.owner = owner;
      this.ownerType = ownerType;
      type = getType(parameterModel.getType());
    }

    @Override
    public String getName() {
      return parameterModel.getName();
    }

    @Override
    public String getDescription() {
      return "top level parameter";
    }
  }

  private class TypedTopLevelParameter extends TopLevelParameter {

    public TypedTopLevelParameter(TopLevelParameter parameter) {
      super(parameter.parameterModel, parameter.owner, parameter.ownerType);
    }

    @Override
    public String getName() {
      return type.getName();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TypedTopLevelParameter) {
        return type.equals(((TypedTopLevelParameter) obj).type);
      }

      return false;
    }

    @Override
    public int hashCode() {
      return type.hashCode();
    }
  }

  private class DescribedReference<T extends Named> extends Reference<T> implements Named, Described {

    private DescribedReference(T value) {
      super(value);
    }

    @Override
    public String getName() {
      return get().getName();
    }

    @Override
    public String getDescription() {
      Named value = get();
      if (value instanceof ConfigurationModel) {
        return "configuration";
      } else if (value instanceof OperationModel) {
        return "operation";
      } else if (value instanceof SourceModel) {
        return "message source";
      } else if (value instanceof ConnectionProviderModel) {
        return "connection provider";
      }

      return "";
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof DescribedReference) {
        return super.equals(obj);
      }

      return false;
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

  }

}

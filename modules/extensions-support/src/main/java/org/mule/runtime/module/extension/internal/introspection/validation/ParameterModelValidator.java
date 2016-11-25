/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.api.meta.model.parameter.ParameterModel.RESERVED_NAMES;
import static org.mule.runtime.extension.api.util.NameUtils.getTopLevelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.xml.dsl.api.XmlModelUtils.supportsTopLevelDeclaration;
import static org.mule.runtime.module.extension.internal.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.declaration.type.annotation.XmlHintsAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.util.ExtensionMetadataTypeUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates that all {@link ParameterModel parameters} provided by the {@link ConfigurationModel configurations},
 * {@link ConnectionProviderModel connection providers} and {@link OperationModel operations} from the {@link ExtensionModel
 * extension} complies with:
 * <ul>
 * <li>The name must not be one of the reserved ones</li>
 * <li>The {@link MetadataType metadataType} must be provided</li>
 * <li>If required, cannot provide a default value</li>
 * <li>The {@link Class} of the parameter must be valid too, that implies that the class shouldn't contain any field with a
 * reserved name.
 * </ul>
 *
 * @since 4.0
 */
public final class ParameterModelValidator implements ModelValidator {

  private SubTypesMappingContainer subTypesMapping;

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {

    subTypesMapping = loadSubtypesMapping(extensionModel);

    MetadataTypeVisitor visitor = new MetadataTypeVisitor() {

      private Set<MetadataType> visitedClasses = new HashSet<>();

      @Override
      public void visitDictionary(DictionaryType dictionaryType) {
        dictionaryType.getKeyType().accept(this);
        dictionaryType.getValueType().accept(this);
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        arrayType.getType().accept(this);
      }

      @Override
      public void visitObject(ObjectType objectType) {

        if (visitedClasses.add(objectType)) {
          for (ObjectFieldType field : objectType.getFields()) {

            String fieldName = field.getKey().getName().getLocalPart();
            if (RESERVED_NAMES.contains(fieldName)) {
              throw new IllegalParameterModelDefinitionException(
                                                                 format("The field named '%s' [%s] from class [%s] cannot have that name since it is a reserved one",
                                                                        fieldName, getId(field.getValue()), getId(objectType)));
            }

            if (supportsGlobalReferences(field)) {
              field.getValue().accept(this);
            }
          }
        }
      }
    };

    String extensionModelName = extensionModel.getName();
    new ExtensionWalker() {

      @Override
      public void onParameter(ParameterizedModel owner, ParameterGroupModel groupModel, ParameterModel model) {
        String ownerName = owner.getName();
        String ownerModelType = getComponentModelTypeName(owner);
        validateParameter(model, visitor, ownerName, ownerModelType, extensionModelName);
        validateParameterGroup(groupModel);
        validateNameCollisionWithTypes(model, ownerName, ownerModelType, extensionModelName,
                                       owner.getAllParameterModels().stream().map(p -> hyphenize(p.getName())).collect(toList()));
      }
    }.walk(extensionModel);
  }

  private void validateParameter(ParameterModel parameterModel, MetadataTypeVisitor visitor, String ownerName,
                                 String ownerModelType, String extensionName) {
    if (RESERVED_NAMES.contains(parameterModel.getName())) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("The parameter in the %s [%s] from the extension [%s] cannot have the name ['%s'] since it is a reserved one",
                                                                ownerModelType, ownerName, extensionName,
                                                                parameterModel.getName()));
    }

    if (parameterModel.getType() == null) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("The parameter [%s] in the %s [%s] from the extension [%s] must provide a type",
                                                                parameterModel.getName(),
                                                                ownerModelType, ownerName, extensionName));
    }

    if (parameterModel.isRequired() && parameterModel.getDefaultValue() != null) {
      throw new IllegalParameterModelDefinitionException(
                                                         format("The parameter [%s] in the %s [%s] from the extension [%s] is required, and must not provide a default value",
                                                                parameterModel.getName(), ownerModelType, ownerName,
                                                                extensionName));
    }

    if ((supportsGlobalReferences(parameterModel) && supportsGlobalReferences(parameterModel.getType())) ||
        supportsInlineDefinition(parameterModel)) {
      parameterModel.getType().accept(visitor);
    }
  }

  private void validateNameCollisionWithTypes(ParameterModel parameterModel, String ownerName, String ownerModelType,
                                              String extensionName, List<String> parameterNames) {
    Optional<MetadataType> subTypeWithNameCollision = subTypesMapping.getSubTypes(parameterModel.getType()).stream()
        .filter(subtype -> parameterNames.contains(getTopLevelTypeName(subtype))).findFirst();
    if (subTypeWithNameCollision.isPresent()) {
      throw new IllegalParameterModelDefinitionException(
                                                         format(
                                                                "The parameter [%s] in the %s [%s] from the extension [%s] can't have the same name as the ClassName or Alias of the declared subType [%s] for parameter [%s]",
                                                                getTopLevelTypeName(subTypeWithNameCollision.get()),
                                                                ownerModelType, ownerName, extensionName,
                                                                getType(subTypeWithNameCollision.get()).getSimpleName(),
                                                                parameterModel.getName()));
    }
  }

  private void validateParameterGroup(ParameterGroupModel groupModel) {
    groupModel.getModelProperty(ParameterGroupModelProperty.class).map(ParameterGroupModelProperty::getDescriptor)
        .ifPresent(group -> {
          if (!isInstantiable(group.getType().getDeclaringClass())) {
            throw new IllegalParameterModelDefinitionException(
                                                               format("The parameter group of type '%s' should be non abstract with a default constructor.",
                                                                      group.getType().getDeclaringClass()));
          }
        });
  }

  private SubTypesMappingContainer loadSubtypesMapping(ExtensionModel extensionModel) {
    return new SubTypesMappingContainer(extensionModel.getSubTypes());
  }

  private boolean supportsGlobalReferences(MetadataType type) {
    return supportsTopLevelDeclaration(type) && ExtensionMetadataTypeUtils.isInstantiable(type);
  }

  private boolean supportsGlobalReferences(ObjectFieldType field) {
    return field.getAnnotation(XmlHintsAnnotation.class).map(XmlHintsAnnotation::allowsReferences).orElse(true);
  }

  private boolean supportsGlobalReferences(ParameterModel parameter) {
    return parameter.getDslModel().allowsReferences();
  }

  private boolean supportsInlineDefinition(ParameterModel parameter) {
    return parameter.getDslModel().allowsInlineDefinition();
  }
}

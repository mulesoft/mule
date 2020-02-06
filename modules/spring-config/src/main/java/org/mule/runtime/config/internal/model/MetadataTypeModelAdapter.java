/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.parameter.ExclusiveParametersModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterRole;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.extension.api.declaration.type.annotation.ExpressionSupportAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.LayoutTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class MetadataTypeModelAdapter implements ParameterizedModel {

  static Optional<MetadataTypeModelAdapter> createMetadataTypeModelAdapterWithSterotype(MetadataType type,
                                                                                        ExtensionModelHelper extensionModelHelper) {
    return type.getAnnotation(StereotypeTypeAnnotation.class)
        .flatMap(sta -> sta.getAllowedStereotypes().stream().findFirst())
        .map(st -> new MetadataTypeModelAdapterWithStereotype(type, st, extensionModelHelper));
  }

  static MetadataTypeModelAdapter createParameterizedTypeModelAdapter(MetadataType type,
                                                                      ExtensionModelHelper extensionModelHelper) {
    return new MetadataTypeModelAdapter(type, extensionModelHelper);
  }

  private final MetadataType type;
  private final MetadataType stringType;

  private MetadataTypeModelAdapter(MetadataType type, ExtensionModelHelper extensionModelHelper) {
    this.type = type;
    this.stringType = extensionModelHelper.findMetadataType(String.class).orElse(null);
  }

  @Override
  public String getName() {
    return getTypeId(type).orElse(type.toString());
  }

  @Override
  public String getDescription() {
    return "MetadataTypeModelAdapter for " + getTypeId(type).orElse(type.toString());
  }

  @Override
  public List<ParameterGroupModel> getParameterGroupModels() {
    if (type instanceof ObjectType) {
      return singletonList(new ObjectTypeAsParameterGroupAdapter((ObjectType) type, stringType));
    } else {
      return emptyList();
    }
  }

  @Override
  public String toString() {
    return "MetadataTypeModelAdapter{" + type.toString() + "}";
  }

  private static class MetadataTypeModelAdapterWithStereotype extends MetadataTypeModelAdapter implements HasStereotypeModel {

    private final StereotypeModel stereotype;

    private MetadataTypeModelAdapterWithStereotype(MetadataType type, StereotypeModel stereotype,
                                                   ExtensionModelHelper extensionModelHelper) {
      super(type, extensionModelHelper);
      this.stereotype = stereotype;
    }

    @Override
    public StereotypeModel getStereotype() {
      return stereotype;
    }

  }

  private static class ObjectTypeAsParameterGroupAdapter implements ParameterGroupModel {

    private final ObjectType adaptedType;
    private final Map<String, ParameterModel> parameterModelsByName;
    private final List<ParameterModel> parameterModels;

    public ObjectTypeAsParameterGroupAdapter(ObjectType adaptedType, MetadataType stringType) {
      this.adaptedType = adaptedType;

      final List<ParameterModel> tempParameterModels = adaptedType.getFields().stream()
          .map(ObjectFieldTypeAsParameterModelAdapter::new)
          .collect(toList());

      this.parameterModelsByName = tempParameterModels
          .stream()
          .collect(toMap(NamedObject::getName, identity()));

      if (!parameterModelsByName.containsKey("name")) {
        final ImmutableParameterModel nameParam = new ImmutableParameterModel("name", "The name of this object in the DSL",
                                                                              stringType,
                                                                              false, true, false, true, NOT_SUPPORTED,
                                                                              null, BEHAVIOUR, null, null, null, null,
                                                                              emptyList(), emptySet());
        this.parameterModelsByName.put("name", nameParam);
        tempParameterModels.add(nameParam);
      }

      this.parameterModels = unmodifiableList(tempParameterModels);
    }

    @Override
    public List<ParameterModel> getParameterModels() {
      return parameterModels;
    }

    @Override
    public Optional<ParameterModel> getParameter(String name) {
      return ofNullable(parameterModelsByName.get(name));
    }

    @Override
    public String getName() {
      return DEFAULT_GROUP_NAME;
    }

    @Override
    public String getDescription() {
      return "";
    }

    @Override
    public Optional<DisplayModel> getDisplayModel() {
      return empty();
    }

    @Override
    public Optional<LayoutModel> getLayoutModel() {
      return empty();
    }

    @Override
    public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
      return empty();
    }

    @Override
    public Set<ModelProperty> getModelProperties() {
      return emptySet();
    }

    @Override
    public List<ExclusiveParametersModel> getExclusiveParametersModels() {
      return emptyList();
    }

    @Override
    public boolean isShowInDsl() {
      return false;
    }

    @Override
    public String toString() {
      return "ObjectTypeAsParameterGroupAdapter{" + adaptedType.toString() + "}";
    }
  }

  private static class ObjectFieldTypeAsParameterModelAdapter implements ParameterModel {

    private final ObjectFieldType wrappedFieldType;
    private final LayoutModel layoutModel;

    public ObjectFieldTypeAsParameterModelAdapter(ObjectFieldType wrappedFieldType) {
      this.wrappedFieldType = wrappedFieldType;
      if (this.wrappedFieldType.getAnnotation(LayoutTypeAnnotation.class).isPresent()) {
        LayoutTypeAnnotation layoutTypeAnnotation = this.wrappedFieldType.getAnnotation(LayoutTypeAnnotation.class).get();
        LayoutModel.LayoutModelBuilder layoutModelBuilder = LayoutModel.builder();
        if (layoutTypeAnnotation.isText()) {
          layoutModelBuilder.asText();
        }
        if (layoutTypeAnnotation.isPassword()) {
          layoutModelBuilder.asPassword();
        }
        if (layoutTypeAnnotation.isQuery()) {
          layoutModelBuilder.asQuery();
        }
        layoutModel = layoutModelBuilder.build();
      } else {
        layoutModel = null;
      }
    }

    @Override
    public String getName() {
      return wrappedFieldType.getKey().getName().getLocalPart();
    }

    @Override
    public String getDescription() {
      return wrappedFieldType.getKey().getDescription().orElse(null);
    }

    @Override
    public <T extends ModelProperty> Optional<T> getModelProperty(Class<T> propertyType) {
      return empty();
    }

    @Override
    public Set<ModelProperty> getModelProperties() {
      return emptySet();
    }

    @Override
    public MetadataType getType() {
      return wrappedFieldType.getValue();
    }

    @Override
    public boolean hasDynamicType() {
      return false;
    }

    @Override
    public Optional<DisplayModel> getDisplayModel() {
      return empty();
    }

    @Override
    public Optional<DeprecationModel> getDeprecationModel() {
      return empty();
    }

    @Override
    public boolean isRequired() {
      return false;
    }

    @Override
    public boolean isOverrideFromConfig() {
      return false;
    }

    @Override
    public ExpressionSupport getExpressionSupport() {
      return wrappedFieldType.getAnnotation(ExpressionSupportAnnotation.class)
          .map(ExpressionSupportAnnotation::getExpressionSupport).orElse(null);
    }

    @Override
    public Object getDefaultValue() {
      return null;
    }

    @Override
    public ParameterDslConfiguration getDslConfiguration() {
      return null;
    }

    @Override
    public ParameterRole getRole() {
      return BEHAVIOUR;
    }

    @Override
    public Optional<LayoutModel> getLayoutModel() {
      return ofNullable(layoutModel);
    }

    @Override
    public List<StereotypeModel> getAllowedStereotypes() {
      return wrappedFieldType.getAnnotation(StereotypeTypeAnnotation.class)
          .map(StereotypeTypeAnnotation::getAllowedStereotypes)
          .orElse(emptyList());
    }

    @Override
    public Optional<ValueProviderModel> getValueProviderModel() {
      return empty();
    }

    @Override
    public boolean isComponentId() {
      return getName().equals("name");
    }

    @Override
    public String toString() {
      return "ObjectFieldTypeAsParameterModelAdapter{" + wrappedFieldType.toString() + "}";
    }
  }
}

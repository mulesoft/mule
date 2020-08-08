/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.BEHAVIOUR;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.SimpleType;
import org.mule.metadata.api.model.StringType;
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
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.LayoutTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.QNameTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.model.parameter.ImmutableParameterModel;
import org.mule.runtime.extension.api.property.QNameModelProperty;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

public class MetadataTypeModelAdapter implements ParameterizedModel {

  /**
   * Adapts the provided {@code type} for treatment as both a {@link ParameterizedModel} and {@link HasStereotypeModel}.
   *
   * @param type the {@link MetadataType} to adapt.
   * @param extensionModelHelper used for actions relative to {@link MetadataType}s.
   * @return the newly created adapter if the provided {@code} is stereotyped, {@link Optional#empty()} if not.
   *
   * @since 4.3
   */
  public static Optional<MetadataTypeModelAdapter> createMetadataTypeModelAdapterWithSterotype(MetadataType type,
                                                                                               ExtensionModelHelper extensionModelHelper) {
    return type.getAnnotation(StereotypeTypeAnnotation.class)
        .flatMap(sta -> sta.getAllowedStereotypes().stream().findFirst())
        .map(st -> new MetadataTypeModelAdapterWithStereotype(type, st, extensionModelHelper));
  }

  /**
   * Adapts the provided {@code singleType} for treatment as a {@link ParameterizedModel}.
   * <p>
   * A new type is created around the provided {@code simpleType} to represent how it is represented in a DSL when it is nested in
   * an array.
   *
   * @param simpleType the {@link MetadataType} to adapt.
   * @param extensionModelHelper used for actions relative to {@link MetadataType}s.
   * @return the newly created adapter.
   *
   * @since 4.4
   */
  public static MetadataTypeModelAdapter createSimpleWrapperTypeModelAdapter(SimpleType simpleType,
                                                                             ExtensionModelHelper extensionModelHelper) {
    ObjectTypeBuilder entryObjectTypeBuilder = new BaseTypeBuilder(MetadataFormat.JAVA).objectType();
    entryObjectTypeBuilder.addField().key(VALUE_ATTRIBUTE_NAME).value(simpleType);

    return new MetadataTypeModelAdapter(entryObjectTypeBuilder.build(), extensionModelHelper) {

      @Override
      public boolean isWrapperFor(MetadataType type) {
        return type instanceof StringType || super.isWrapperFor(type);
      }
    };
  }

  /**
   * Adapts the provided types representing the key/values of a map for treatment as a {@link ParameterizedModel}.
   * <p>
   * A new type is created around the provided types with the given paramNames to represent how it is represented in a DSL when it
   * is nested in a map.
   *
   * @param keyParamName the name of the attribute in the DSL containing the entry key.
   * @param simpleKeyType the {@link MetadataType} of the entry key
   * @param valueParamName the name of the attribute in the DSL containing the entry value.
   * @param simpleValueType the {@link MetadataType} of the entry value
   * @param extensionModelHelper used for actions relative to {@link MetadataType}s.
   * @return the newly created adapter.
   *
   * @since 4.4
   */
  public static MetadataTypeModelAdapter createKeyValueWrapperTypeModelAdapter(String keyParamName,
                                                                               MetadataType simpleKeyType,
                                                                               String valueParamName,
                                                                               MetadataType simpleValueType,
                                                                               ExtensionModelHelper extensionModelHelper) {
    ObjectTypeBuilder entryObjectTypeBuilder = new BaseTypeBuilder(MetadataFormat.JAVA).objectType();
    entryObjectTypeBuilder.addField().key(keyParamName).value(simpleKeyType);
    entryObjectTypeBuilder.addField().key(valueParamName).value(simpleValueType);

    return new MetadataTypeModelAdapter(entryObjectTypeBuilder.build(), extensionModelHelper);
  }

  /**
   * Adapts the provided {@code type} for treatment as a {@link ParameterizedModel}.
   *
   * @param type the {@link MetadataType} to adapt.
   * @param extensionModelHelper used for actions relative to {@link MetadataType}s.
   * @return the newly created adapter.
   *
   * @since 4.3
   */
  public static MetadataTypeModelAdapter createParameterizedTypeModelAdapter(MetadataType type,
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

  public MetadataType getType() {
    return type;
  }

  public boolean isWrapperFor(MetadataType type) {
    return this.type.equals(type);
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
          .flatMap(wrappedFieldType -> wrappedFieldType.getAnnotation(FlattenedTypeAnnotation.class).isPresent()
              && wrappedFieldType.getValue() instanceof ObjectType
                  ? ((ObjectType) (wrappedFieldType.getValue())).getFields().stream()
                  : Stream.of(wrappedFieldType))
          .map(field -> new ObjectFieldTypeAsParameterModelAdapter(field, adaptedType.getAnnotation(QNameTypeAnnotation.class)
              .map(QNameTypeAnnotation::getValue)))
          .sorted(comparing(ObjectFieldTypeAsParameterModelAdapter::getName))
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

    private static final ParameterDslConfiguration DSL_CONFIG = ParameterDslConfiguration.builder()
        .allowsInlineDefinition(true)
        .allowsReferences(false)
        .allowTopLevelDefinition(false)
        .build();

    private final ObjectFieldType wrappedFieldType;
    private final Map<Class<? extends ModelProperty>, ModelProperty> modelProperties = new LinkedHashMap<>();
    private final LayoutModel layoutModel;

    public ObjectFieldTypeAsParameterModelAdapter(ObjectFieldType wrappedFieldType, Optional<QName> ownerCustomQName) {
      this.wrappedFieldType = wrappedFieldType;

      ownerCustomQName
          .map(qName -> new QNameModelProperty(new QName(qName.getNamespaceURI(),
                                                         wrappedFieldType.getKey().getName().getLocalPart(), qName.getPrefix())))
          .ifPresent(qnmp -> modelProperties.put(qnmp.getClass(), qnmp));

      Optional<LayoutTypeAnnotation> optionalLayoutTypeAnnotation =
          this.wrappedFieldType.getAnnotation(LayoutTypeAnnotation.class);
      if (optionalLayoutTypeAnnotation.isPresent()) {
        LayoutTypeAnnotation layoutTypeAnnotation = optionalLayoutTypeAnnotation.get();
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
      requireNonNull(propertyType);
      return ofNullable((T) modelProperties.get(propertyType));
    }

    @Override
    public Set<ModelProperty> getModelProperties() {
      return unmodifiableSet(new LinkedHashSet<>(modelProperties.values()));
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
      return DSL_CONFIG;
    }

    @Override
    public ParameterRole getRole() {
      return getLayoutModel()
          .map(layoutModel -> layoutModel.isText() ? CONTENT : BEHAVIOUR)
          .orElse(BEHAVIOUR);
    }

    @Override
    public Optional<LayoutModel> getLayoutModel() {
      return ofNullable(layoutModel);
    }

    @Override
    public List<StereotypeModel> getAllowedStereotypes() {
      return wrappedFieldType.getAnnotation(StereotypeTypeAnnotation.class)
          .map(StereotypeTypeAnnotation::getAllowedStereotypes)
          .orElseGet(() -> wrappedFieldType.getValue().getAnnotation(StereotypeTypeAnnotation.class)
              .map(StereotypeTypeAnnotation::getAllowedStereotypes)
              .orElse(emptyList()));
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

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_SCHEDULER_FIXED_FREQUENCY;
import static org.mule.runtime.ast.api.ComponentAst.BODY_RAW_PARAM_NAME;
import static org.mule.runtime.config.internal.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.internal.model.ApplicationModel.FIXED_FREQUENCY_STRATEGY_IDENTIFIER;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;

import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.AttributeDefinition;
import org.mule.runtime.dsl.api.component.AttributeDefinitionVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;
import org.mule.runtime.dsl.api.component.SetterAttributeDefinition;
import org.mule.runtime.dsl.api.component.TypeConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * Based on the object building definition provided by {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition} and
 * the user configuration defined in {@link ComponentAst} it populates all the spring
 * {@link org.springframework.beans.factory.config.BeanDefinition} attributes using the helper class
 * {@link BeanDefinitionBuilderHelper}. {@link BeanDefinitionBuilderHelper}.
 *
 * @since 4.0
 */
class ComponentConfigurationBuilder<T> {

  private static final Logger logger = LoggerFactory.getLogger(ComponentConfigurationBuilder.class);

  private final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper;
  private final ObjectReferencePopulator objectReferencePopulator = new ObjectReferencePopulator();
  private final List<ComponentValue> complexParameters;
  private final ComponentAst ownerComponent;
  private final ComponentAst componentModel;
  private final CreateBeanDefinitionRequest createBeanDefinitionRequest;
  private final ComponentBuildingDefinition<T> componentBuildingDefinition;
  private final ParameterGroupUtils parameterGroupUtils = new ParameterGroupUtils();

  public ComponentConfigurationBuilder(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                       ComponentAst ownerComponent, ComponentAst componentModel,
                                       CreateBeanDefinitionRequest createBeanDefinitionRequest,
                                       ComponentBuildingDefinition<T> componentBuildingDefinition,
                                       BeanDefinitionBuilderHelper beanDefinitionBuilderHelper) {
    this.ownerComponent = ownerComponent;
    this.componentModel = componentModel;
    this.createBeanDefinitionRequest = createBeanDefinitionRequest;
    this.componentBuildingDefinition = componentBuildingDefinition;
    this.beanDefinitionBuilderHelper = beanDefinitionBuilderHelper;
    this.complexParameters = collectComplexParametersWithTypes(springComponentModels, ownerComponent, componentModel);
  }

  public void processConfiguration() {
    for (SetterAttributeDefinition setterAttributeDefinition : componentBuildingDefinition.getSetterParameterDefinitions()) {
      AttributeDefinition attributeDefinition = setterAttributeDefinition.getAttributeDefinition();
      attributeDefinition.accept(setterVisitor(setterAttributeDefinition.getAttributeName(), attributeDefinition));
    }
    for (AttributeDefinition attributeDefinition : componentBuildingDefinition.getConstructorAttributeDefinition()) {
      attributeDefinition.accept(constructorVisitor());
    }
  }

  private List<ComponentValue> collectComplexParametersWithTypes(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                 ComponentAst ownerComponent, ComponentAst componentModel) {
    /*
     * TODO: MULE-9638 This ugly code is required since we need to get the object type from the bean definition. This code will go
     * away one we remove the old parsing method.
     */
    return componentModel.directChildrenStream()
        .map(springComponentModels::get)
        .filter(Objects::nonNull)
        .map(springModel -> {
          Class<?> beanDefinitionType = resolveBeanDefinitionType(springModel);
          Object bean = springModel.getBeanDefinition() != null
              ? springModel.getBeanDefinition()
              : springModel.getBeanReference();
          return new ComponentValue(springModel.getComponent(), beanDefinitionType, bean);
        })
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private Class<?> resolveBeanDefinitionType(SpringComponentModel springModel) {
    // When it comes from old model it does not have the type set
    if (springModel.getType() != null) {
      return springModel.getType();
    }

    if (springModel.getBeanDefinition() == null) {
      // Some component do not have a bean definition since the element parsing is ignored. i.e: annotations
      return null;
    }

    try {
      String beanClassName = springModel.getBeanDefinition().getBeanClassName();
      if (beanClassName != null) {
        return org.apache.commons.lang3.ClassUtils.getClass(beanClassName);
      } else {
        // Happens in case of spring:property
        return Object.class;
      }
    } catch (ClassNotFoundException e) {
      logger.debug("Exception trying to determine ComponentModel type: ", e);
      return Object.class;
    }
  }

  private ConfigurableAttributeDefinitionVisitor constructorVisitor() {
    return new ConfigurableAttributeDefinitionVisitor(beanDefinitionBuilderHelper::addConstructorValue);
  }

  private ConfigurableAttributeDefinitionVisitor setterVisitor(String propertyName, AttributeDefinition attributeDefinition) {
    DefaultValueVisitor defaultValueVisitor = new DefaultValueVisitor();
    attributeDefinition.accept(defaultValueVisitor);
    Optional<Object> defaultValue = defaultValueVisitor.getDefaultValue();
    return new ConfigurableAttributeDefinitionVisitor(value -> {
      if (isPropertySetWithUserConfigValue(propertyName, defaultValue, value)) {
        return;
      }
      beanDefinitionBuilderHelper.forProperty(propertyName).addValue(value);
    });
  }

  private boolean isPropertySetWithUserConfigValue(String propertyName, Optional<Object> defaultValue, Object value) {
    return defaultValue.isPresent() && defaultValue.get().equals(value)
        && beanDefinitionBuilderHelper.hasValueForProperty(propertyName);
  }

  /**
   * Process a single {@link AttributeDefinition} from a {@link ComponentBuildingDefinition} and uses an invokes a
   * {@code Consumer} when the value is a bean definition or a different {@code Consumer} if the value is a bean reference.
   */
  private class ConfigurableAttributeDefinitionVisitor implements AttributeDefinitionVisitor {

    private final Consumer<Object> valueConsumer;

    /**
     * @param valueConsumer consumer for handling a bean definition
     */
    ConfigurableAttributeDefinitionVisitor(Consumer<Object> valueConsumer) {
      this.valueConsumer = valueConsumer;
    }

    @Override
    public void onReferenceObject(Class<?> objectType) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onReferenceObject(objectType);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onReferenceSimpleParameter(final String configAttributeName) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onReferenceSimpleParameter(configAttributeName);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onSoftReferenceSimpleParameter(String softReference) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onSoftReferenceSimpleParameter(softReference);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onReferenceFixedParameter(String reference) {
      valueConsumer.accept(new RuntimeBeanReference(reference));
    }

    @Override
    public void onFixedValue(Object value) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onFixedValue(value);
      valueConsumer.accept(value);
    }

    @Override
    public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onConfigurationParameter(parameterName, defaultValue, typeConverter);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onReferenceConfigurationParameter(String parameterName, Object defaultValue,
                                                  Optional<TypeConverter> typeConverter) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onConfigurationParameter(parameterName, defaultValue, typeConverter);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onUndefinedSimpleParameters() {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onUndefinedSimpleParameters();
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onUndefinedComplexParameters() {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onUndefinedComplexParameters();
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifier) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onComplexChildCollection(type, wrapperIdentifier);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onComplexChildMap(keyType, valueType, wrapperIdentifier);
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier, Optional<String> childIdentifier) {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onComplexChild(type, wrapperIdentifier, childIdentifier);
      Object value = valueExtractor.getValue();
      if (value != null) {
        valueConsumer.accept(value);
      }
    }

    @Override
    public void onValueFromTextContent() {
      ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
      valueExtractor.onValueFromTextContent();
      valueConsumer.accept(valueExtractor.getValue());
    }

    @Override
    public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
      ManagedMap managedMap = new ManagedMap();
      for (KeyAttributeDefinitionPair definition : definitions) {
        ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
        definition.getAttributeDefinition().accept(valueExtractor);
        Object value = valueExtractor.getValue();
        if (value != null) {
          managedMap.put(definition.getKey(), value);
        }
      }
      valueConsumer.accept(managedMap);
    }

  }

  /**
   * {code AttributeDefinitionVisitor} that extracts the value from the {@code ComponentModel}
   */
  private class ValueExtractorAttributeDefinitionVisitor implements AttributeDefinitionVisitor {

    private Object value;

    public Object getValue() {
      return value;
    }

    @Override
    public void onReferenceObject(Class<?> objectType) {
      objectReferencePopulator.populate(objectType, referenceId -> this.value = new RuntimeBeanReference(referenceId));
    }

    @Override
    public void onReferenceSimpleParameter(final String configAttributeName) {
      if (!componentBuildingDefinition.getIgnoredConfigurationParameters().contains(configAttributeName)) {
        getParameterValue(configAttributeName, null)
            .ifPresent(reference -> this.value = new RuntimeBeanReference((String) reference));
      }
    }

    @Override
    public void onSoftReferenceSimpleParameter(String softReference) {
      if (!componentBuildingDefinition.getIgnoredConfigurationParameters().contains(softReference)) {
        getParameterValue(softReference, null)
            .ifPresent(reference -> this.value = reference);
      }
    }

    @Override
    public void onReferenceFixedParameter(String reference) {
      this.value = new RuntimeBeanReference(reference);
    }

    @Override
    public void onFixedValue(Object value) {
      this.value = value;
    }

    @Override
    public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter) {
      if (!componentBuildingDefinition.getIgnoredConfigurationParameters().contains(parameterName)) {
        getParameterValue(parameterName, defaultValue)
            .map(parameterValue -> typeConverter.isPresent() ? typeConverter.get().convert(parameterValue) : parameterValue)
            .ifPresent(convertedParameterValue -> this.value = convertedParameterValue);
      }
    }

    private Optional<Object> getParameterValue(String parameterName, Object defaultValue) {
      ComponentParameterAst parameter = ownerComponent.getModel(ParameterizedModel.class)
          .map(ownerComponentModel -> {
            // For sources, we need to account for the case where parameters in the callbacks may have colliding names.
            // This logic ensures that the parameter fetching logic is consistent with the logic that handles this scenario in
            // previous implementations.
            int ownerIndex = createBeanDefinitionRequest.getComponentModelHierarchy().indexOf(ownerComponent);
            final ComponentAst possibleGroup =
                ownerIndex + 1 >= createBeanDefinitionRequest.getComponentModelHierarchy().size()
                    ? componentModel
                    : createBeanDefinitionRequest.getComponentModelHierarchy().get(ownerIndex + 1);
            if (ownerComponent != componentModel && ownerComponentModel instanceof SourceModel) {
              return parameterGroupUtils.getSourceCallbackAwareParameter(ownerComponent, parameterName, possibleGroup,
                                                                         (SourceModel) ownerComponentModel);
            } else {
              Optional<ParameterGroupModel> groupModelOptional =
                  parameterGroupUtils.getParameterGroupModel(ownerComponent, parameterName, possibleGroup,
                                                             ownerComponentModel.getParameterGroupModels());
              ComponentParameterAst p;
              if (groupModelOptional.isPresent()) {
                p = ownerComponent.getParameter(groupModelOptional.get().getName(), parameterName);
              } else {
                p = ownerComponent.getParameter(parameterName);
              }

              if (p == null) {
                // XML SDK 1 allows for hyphenized names in parameters, so need to account for those.
                return ownerComponent.getParameter(componentModel.getIdentifier().getName());
              }

              return p;
            }
          })
          .orElse(null);

      Object parameterValue;
      if (parameter == null) {
        // Fallback for test components that do not have an extension model.
        parameterValue = componentModel.getRawParameterValue(parameterName)
            .map(v -> (Object) v)
            .orElse(defaultValue);
      } else if ("frequency".equals(parameterName)
          && ownerComponent.getIdentifier().equals(FIXED_FREQUENCY_STRATEGY_IDENTIFIER)
          && parameter.isDefaultValue()) {
        // Account for inconsistency in the extension model. Ref: MULE-18262
        parameterValue = getDefaultSchedulerFixedFrequency();
      } else {
        parameterValue = parameter.getValue()
            .mapLeft(expr -> DEFAULT_EXPRESSION_PREFIX + expr + DEFAULT_EXPRESSION_POSTFIX)
            .getValue()
            .orElse(null);

        if (defaultValue != null && parameterValue == null) {
          logger
              .warn("Paramerter {} from extension {} has a defaultValue configured in the componentBuildingDefinition but not in the extensionModel.",
                    parameterName, ownerComponent.getIdentifier().getNamespace());
          parameterValue = defaultValue;
        }
      }

      if (parameterValue instanceof ComponentAst || parameterValue instanceof Collection) {
        // Do not set complex parameters here
        return empty();
      }

      return ofNullable(parameterValue);
    }

    private long getDefaultSchedulerFixedFrequency() {
      String freq = getProperty(DEFAULT_SCHEDULER_FIXED_FREQUENCY, "1000");
      try {
        return Long.valueOf(freq);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(format("Invalid value for System Property %s. A long number was expected but '%s' found instead",
                                                  DEFAULT_SCHEDULER_FIXED_FREQUENCY, freq));
      }
    }

    @Override
    public void onReferenceConfigurationParameter(String parameterName, Object defaultValue,
                                                  Optional<TypeConverter> typeConverter) {
      onConfigurationParameter(parameterName, defaultValue, typeConverter);
    }

    @Override
    public void onUndefinedSimpleParameters() {
      this.value = componentModel.getModel(ParameterizedModel.class)
          .map(pm -> componentModel.getParameters().stream()
              .filter(param -> !componentBuildingDefinition.getIgnoredConfigurationParameters()
                  .contains(param.getModel().getName()))
              .filter(param -> param.getResolvedRawValue() != null)
              .collect(toMap(param -> param.getModel().getName(), ComponentParameterAst::getResolvedRawValue)))
          .orElse(null);
    }

    @Override
    public void onUndefinedComplexParameters() {
      this.value = constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(complexParameters));
    }

    @Override
    public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifier) {
      Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, wrapperIdentifier);
      List<ComponentValue> matchingComponentValues = complexParameters.stream()
          .filter(matchesTypeAndIdentifierPredicate)
          .collect(toList());

      matchingComponentValues.stream().forEach(complexParameters::remove);
      if (wrapperIdentifier.isPresent() && !matchingComponentValues.isEmpty()) {
        this.value = matchingComponentValues.get(0).getBean();
      } else {
        if (!matchingComponentValues.isEmpty()) {
          this.value = constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(matchingComponentValues));
        }
      }
    }

    @Override
    public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier) {
      complexParameters.stream()
          .filter(getTypeAndIdentifierPredicate(MapFactoryBean.class, ofNullable(wrapperIdentifier)))
          .findFirst()
          .ifPresent(componentValue -> {
            complexParameters.remove(componentValue);
            value = componentValue.getBean();
          });
    }

    @Override
    public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier, Optional<String> childIdentifier) {
      Optional<String> identifier = wrapperIdentifier.isPresent() ? wrapperIdentifier : childIdentifier;
      Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, identifier);
      Optional<ComponentValue> value = complexParameters.stream().filter(matchesTypeAndIdentifierPredicate).findFirst();
      value.ifPresent(beanDefinitionTypePair -> {
        complexParameters.remove(beanDefinitionTypePair);
        Object bean = beanDefinitionTypePair.getBean();
        this.value = bean;
      });
    }

    private Predicate<ComponentValue> getTypeAndIdentifierPredicate(Class<?> type, Optional<String> identifierOptional) {
      return componentValue -> {
        AtomicReference<Boolean> matchesIdentifier = new AtomicReference<>(true);
        identifierOptional.ifPresent(wrapperIdentifier -> matchesIdentifier
            .set(wrapperIdentifier.equals(componentValue.getComponentModel().getIdentifier().getName())));
        return matchesIdentifier.get() && (areMatchingTypes(type, componentValue.getType())
            || ((areMatchingTypes(Map.class, componentValue.getType()) && areMatchingTypes(MapFactoryBean.class, type))));
      };
    }

    @Override
    public void onValueFromTextContent() {
      this.value = componentModel.getRawParameterValue(BODY_RAW_PARAM_NAME).orElse(null);
    }

    @Override
    public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
      for (KeyAttributeDefinitionPair definition : definitions) {
        definition.getAttributeDefinition().accept(this);
      }
    }
  }

  private ManagedList constructManagedList(List<Object> beans) {
    ManagedList managedList = new ManagedList();
    managedList.addAll(beans);
    return managedList;
  }

  private List<Object> fromBeanDefinitionTypePairToBeanDefinition(List<ComponentValue> undefinedComplexParameters) {
    return undefinedComplexParameters.stream().map(ComponentValue::getBean).collect(toList());
  }

}

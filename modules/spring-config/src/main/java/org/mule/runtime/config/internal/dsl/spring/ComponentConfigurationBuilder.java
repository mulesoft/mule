/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.MuleSystemProperties.DEFAULT_SCHEDULER_FIXED_FREQUENCY;
import static org.mule.runtime.config.internal.dsl.spring.CommonComponentBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.internal.dsl.spring.SimpleTypeBeanParamDefinitionCreator.resolveParamValue;
import static org.mule.runtime.config.internal.model.ApplicationModel.FIXED_FREQUENCY_STRATEGY_IDENTIFIER;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
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
import java.util.stream.Stream;

import org.slf4j.Logger;
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

  private static final Logger LOGGER = getLogger(ComponentConfigurationBuilder.class);
  private static final String GLOBAL_FUNCTIONS_START_TAG = "<global-functions><![CDATA[";
  private static final String GLOBAL_FUNCTIONS_END_TAG = "]]></global-functions>";

  private final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper;
  private final ObjectReferencePopulator objectReferencePopulator = new ObjectReferencePopulator();
  private final List<ComponentValue> complexParameters;
  private final ComponentAst ownerComponent;
  private final ComponentAst component;
  private final CreateBeanDefinitionRequest<T> createBeanDefinitionRequest;

  private final boolean disableTrimWhitespaces;

  public ComponentConfigurationBuilder(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                       ComponentAst ownerComponent, ComponentAst component,
                                       CreateBeanDefinitionRequest<T> request,
                                       BeanDefinitionBuilderHelper beanDefinitionBuilderHelper,
                                       boolean disableTrimWhitespaces) {
    this.ownerComponent = ownerComponent;
    this.component = request.resolveConfigurationComponent();
    this.createBeanDefinitionRequest = request;
    this.beanDefinitionBuilderHelper = beanDefinitionBuilderHelper;
    this.complexParameters = collectComplexParametersWithTypes(springComponentModels, ownerComponent, component);
    this.disableTrimWhitespaces = disableTrimWhitespaces;
  }

  public void processConfiguration() {
    for (SetterAttributeDefinition setterAttributeDefinition : createBeanDefinitionRequest.getComponentBuildingDefinition()
        .getSetterParameterDefinitions()) {
      AttributeDefinition attributeDefinition = setterAttributeDefinition.getAttributeDefinition();
      attributeDefinition.accept(setterVisitor(setterAttributeDefinition.getAttributeName(), attributeDefinition));
    }
    for (AttributeDefinition attributeDefinition : createBeanDefinitionRequest.getComponentBuildingDefinition()
        .getConstructorAttributeDefinition()) {
      attributeDefinition.accept(constructorVisitor());
    }
  }

  private List<ComponentValue> collectComplexParametersWithTypes(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                 ComponentAst ownerComponent, ComponentAst component) {
    /*
     * TODO: MULE-9638 This ugly code is required since we need to get the object type from the bean definition. This code will go
     * away one we remove the old parsing method.
     */

    final Stream<SpringComponentModel> baseStream = component != null
        ? concat(createBeanDefinitionRequest.getParamsModels().stream(),
                 component.directChildrenStream()
                     .map(springComponentModels::get)
                     .filter(Objects::nonNull))
        : createBeanDefinitionRequest.getParamsModels().stream();

    return baseStream
        .map(springModel -> new ComponentValue(springModel.getComponentIdentifier(),
                                               resolveBeanDefinitionType(springModel),
                                               springModel.getBeanDefinition() != null
                                                   ? springModel.getBeanDefinition()
                                                   : springModel.getBeanReference()))
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
      LOGGER.debug("Exception trying to determine ComponentModel type: ", e);
      return Object.class;
    }
  }

  private ConfigurableAttributeDefinitionVisitor constructorVisitor() {
    return new ConfigurableAttributeDefinitionVisitor(beanDefinitionBuilderHelper::addConstructorValue);
  }

  private ConfigurableAttributeDefinitionVisitor setterVisitor(String propertyName, AttributeDefinition attributeDefinition) {
    return new ConfigurableAttributeDefinitionVisitor(value -> {
      if (isPropertySetWithUserConfigValue(propertyName, getDefaultValue(attributeDefinition), value)) {
        return;
      }
      beanDefinitionBuilderHelper.forProperty(propertyName).addValue(value);
    });
  }

  private Optional<Object> getDefaultValue(AttributeDefinition attributeDefinition) {
    DefaultValueVisitor defaultValueVisitor = new DefaultValueVisitor();
    attributeDefinition.accept(defaultValueVisitor);
    return defaultValueVisitor.getDefaultValue();
  }

  private boolean isPropertySetWithUserConfigValue(String propertyName, Optional<Object> defaultValue, Object value) {
    return defaultValue.isPresent() && defaultValue.get().equals(value)
        && beanDefinitionBuilderHelper.hasValueForProperty(propertyName);
  }

  /**
   * Process a single {@link AttributeDefinition} from a {@link ComponentBuildingDefinition} and uses a {@code Consumer} when the
   * value is a bean definition or a different {@code Consumer} if the value is a bean reference.
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
      if (!createBeanDefinitionRequest.getComponentBuildingDefinition().getIgnoredConfigurationParameters()
          .contains(configAttributeName)) {
        getParameterValue(configAttributeName, null)
            .ifPresent(reference -> this.value = new RuntimeBeanReference((String) reference));
      }
    }

    @Override
    public void onSoftReferenceSimpleParameter(String softReference) {
      if (!createBeanDefinitionRequest.getComponentBuildingDefinition().getIgnoredConfigurationParameters()
          .contains(softReference)) {
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
      if (!createBeanDefinitionRequest.getComponentBuildingDefinition().getIgnoredConfigurationParameters()
          .contains(parameterName)) {
        this.value = getParameterValue(parameterName, defaultValue)
            .map(parameterValue -> typeConverter
                .map(tc -> tc.convert(parameterValue))
                .orElse(parameterValue))
            .orElse(null);
      }
    }

    private Optional<Object> getParameterValue(String parameterName, Object defaultValue) {
      ComponentParameterAst parameter = ownerComponent.getModel(ParameterizedModel.class)
          .map(ownerComponentModel -> doResolveParameter(createBeanDefinitionRequest.getParameter(parameterName)))
          .orElse(null);

      Object parameterValue;
      if (parameter == null) {
        parameterValue = defaultValue;
      } else if (shouldSetDefaultFrequencyForFixedFrequencyStrategy(parameterName, parameter)) {
        // Account for inconsistency in the extension model. Ref: MULE-18262
        parameterValue = getDefaultSchedulerFixedFrequency();
      } else {
        parameterValue = resolveParamValue(parameter, disableTrimWhitespaces, false);

        if (defaultValue != null && parameterValue == null) {
          LOGGER
              .warn("Parameter {} from extension {} has a defaultValue configured in the componentBuildingDefinition but not in the extensionModel.",
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

    private ComponentParameterAst doResolveParameter(ComponentParameterAst param) {
      if (param == null && component != null) {
        // XML SDK 1 allows for hyphenized names in parameters, so need to account for those.
        return ownerComponent.getParameter(DEFAULT_GROUP_NAME, component.getIdentifier().getName());
      }

      return param;
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
      this.value = component.getModel(ParameterizedModel.class)
          .map(pm -> component.getParameters().stream()
              .filter(param -> !createBeanDefinitionRequest.getComponentBuildingDefinition().getIgnoredConfigurationParameters()
                  .contains(param.getModel().getName()))
              .filter(param -> param.getResolvedRawValue() != null)
              .collect(toMap(param -> param.getModel().getName(),
                             ComponentParameterAst::getResolvedRawValue)))
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
            .set(wrapperIdentifier.equals(componentValue.getComponentIdentifier().getName())));
        return matchesIdentifier.get() && (areMatchingTypes(type, componentValue.getType())
            || ((areMatchingTypes(Map.class, componentValue.getType()) && areMatchingTypes(MapFactoryBean.class, type))));
      };
    }

    @Override
    public void onValueFromTextContent() {
      ComponentAst componentAst = createBeanDefinitionRequest.getComponent();
      Optional<String> value = Optional.empty();
      if (componentAst != null && componentAst.getMetadata() != null) {
        value = componentAst.getMetadata().getSourceCode();
      }

      if (value != null && value.isPresent() && value.get().startsWith(GLOBAL_FUNCTIONS_START_TAG)) {
        Optional<String> data =
            value.map(val -> val.substring(GLOBAL_FUNCTIONS_START_TAG.length(), val.indexOf(GLOBAL_FUNCTIONS_END_TAG)));
        this.value = data.orElse("");
      } else {
        getParameterValue(((CreateParamBeanDefinitionRequest) createBeanDefinitionRequest).getParam().getModel().getName(), null)
            .ifPresent(v -> this.value = v);
      }
    }

    @Override
    public void onMultipleValues(KeyAttributeDefinitionPair[] definitions) {
      for (KeyAttributeDefinitionPair definition : definitions) {
        definition.getAttributeDefinition().accept(this);
      }
    }
  }

  private boolean shouldSetDefaultFrequencyForFixedFrequencyStrategy(String parameterName, ComponentParameterAst parameter) {
    return "frequency".equals(parameterName)
        && ownerComponent.getIdentifier().equals(FIXED_FREQUENCY_STRATEGY_IDENTIFIER)
        && parameter.isDefaultValue() && parameter.getResolvedRawValue() == null;
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

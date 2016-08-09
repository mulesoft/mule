/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.spring.util.ProcessingStrategyUtils.parseProcessingStrategy;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair;
import org.mule.runtime.config.spring.dsl.api.SetterAttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.AttributeDefinitionVisitor;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Based on the object building definition provided by {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition}
 * and the user configuration defined in {@link org.mule.runtime.config.spring.dsl.model.ComponentModel} it populates all the
 * spring {@link org.springframework.beans.factory.config.BeanDefinition} attributes using the helper class {@link org.mule.runtime.config.spring.dsl.spring.BeanDefinitionBuilderHelper}.
 *
 * @since 4.0
 */
class ComponentConfigurationBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(ComponentConfigurationBuilder.class);

    private final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper;
    private final ObjectReferencePopulator objectReferencePopulator = new ObjectReferencePopulator();
    private final List<ComponentValue> complexParameters;
    private final Map<String, String> simpleParameters;
    private final ComponentModel componentModel;
    private final ComponentBuildingDefinition componentBuildingDefinition;

    public ComponentConfigurationBuilder(ComponentModel componentModel,
                                         ComponentBuildingDefinition componentBuildingDefinition,
                                         BeanDefinitionBuilderHelper beanDefinitionBuilderHelper)
    {
        this.componentModel = componentModel;
        this.componentBuildingDefinition = componentBuildingDefinition;
        this.beanDefinitionBuilderHelper = beanDefinitionBuilderHelper;
        this.simpleParameters = new HashMap(componentModel.getParameters());
        this.complexParameters = collectComplexParametersWithTypes(componentModel);
    }

    public void processConfiguration()
    {
        componentBuildingDefinition.getIgnoredConfigurationParameters().stream().forEach(simpleParameters::remove);

        for (SetterAttributeDefinition setterAttributeDefinition : componentBuildingDefinition.getSetterParameterDefinitions())
        {
            AttributeDefinition attributeDefinition = setterAttributeDefinition.getAttributeDefinition();
            attributeDefinition.accept(setterVisitor(setterAttributeDefinition.getAttributeName(), attributeDefinition));
        }
        for (AttributeDefinition attributeDefinition : componentBuildingDefinition.getConstructorAttributeDefinition())
        {
            attributeDefinition.accept(constructorVisitor());
        }
    }

    private List<ComponentValue> collectComplexParametersWithTypes(ComponentModel componentModel)
    {
        /*
         * TODO: MULE-9638 This ugly code is required since we need to get the object type from the bean definition.
         * This code will go away one we remove the old parsing method.
         */
        return componentModel.getInnerComponents().stream().map(cdm -> {
            //When it comes from old model it does not have the type set
            Class<?> beanDefinitionType = cdm.getType();
            if (beanDefinitionType == null)
            {
                if (cdm.getBeanDefinition() == null)
                {
                    //Some component do not have a bean definition since the element parsing is ignored. i.e: annotations
                    return null;
                }
                else
                {
                    try
                    {
                        String beanClassName = cdm.getBeanDefinition().getBeanClassName();
                        if (beanClassName != null)
                        {
                            beanDefinitionType = ClassUtils.getClass(beanClassName);
                        }
                        else
                        {
                            //Happens in case of spring:property
                            beanDefinitionType = Object.class;
                        }
                    }
                    catch (ClassNotFoundException e)
                    {
                        logger.debug("Exception trying to determine ComponentModel type: ", e);
                        beanDefinitionType = Object.class;
                    }
                }
            }
            Object bean = cdm.getBeanDefinition() != null ? cdm.getBeanDefinition() : cdm.getBeanReference();
            return new ComponentValue(cdm, beanDefinitionType, bean);
        })
        .filter(beanDefinitionTypePair -> beanDefinitionTypePair != null)
        .collect(toList());
    }

    private ConfigurableAttributeDefinitionVisitor constructorVisitor()
    {
        return new ConfigurableAttributeDefinitionVisitor(beanDefinitionBuilderHelper::addConstructorValue);
    }

    private ConfigurableAttributeDefinitionVisitor setterVisitor(String propertyName, AttributeDefinition attributeDefinition)
    {
        DefaultValueVisitor defaultValueVisitor = new DefaultValueVisitor();
        attributeDefinition.accept(defaultValueVisitor);
        Optional<Object> defaultValue = defaultValueVisitor.getDefaultValue();
        return new ConfigurableAttributeDefinitionVisitor(value -> {
            if (isPropertySetWithUserConfigValue(propertyName, defaultValue, value))
            {
                return;
            }
            beanDefinitionBuilderHelper.forProperty(propertyName).addValue(value);
        });
    }

    private boolean isPropertySetWithUserConfigValue(String propertyName, Optional<Object> defaultValue, Object value)
    {
        return defaultValue.isPresent() && defaultValue.get().equals(value) && beanDefinitionBuilderHelper.hasValueForProperty(propertyName);
    }

    /**
     * Process a single {@link AttributeDefinition} from a {@link ComponentBuildingDefinition} and
     * uses an invokes a {@code Consumer} when the value is a bean definition or a different {@code Consumer}
     * if the value is a bean reference.
     */
    private class ConfigurableAttributeDefinitionVisitor implements AttributeDefinitionVisitor
    {

        private final Consumer<Object> valueConsumer;

        /**
         * @param valueConsumer consumer for handling a bean definition
         */
        ConfigurableAttributeDefinitionVisitor(Consumer<Object> valueConsumer)
        {
            this.valueConsumer = valueConsumer;
        }

        @Override
        public void onReferenceObject(Class<?> objectType)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onReferenceObject(objectType);
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onReferenceSimpleParameter(configAttributeName);
            Object value = valueExtractor.getValue();
            if (value != null)
            {
                valueConsumer.accept(value);
            }
            else
            {
                valueConsumer.accept(null);
            }
        }

        @Override
        public void onFixedValue(Object value)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onFixedValue(value);
            valueConsumer.accept(value);
        }

        @Override
        public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onConfigurationParameter(parameterName, defaultValue, typeConverter);
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onUndefinedSimpleParameters()
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onUndefinedSimpleParameters();
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onUndefinedComplexParameters()
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onUndefinedComplexParameters();
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifier)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onComplexChildCollection(type, wrapperIdentifier);
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onComplexChildMap(keyType, valueType, wrapperIdentifier);
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier, Optional<String> childIdentifier)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onComplexChild(type, wrapperIdentifier, childIdentifier);
            Object value = valueExtractor.getValue();
            if (value != null)
            {
                valueConsumer.accept(value);
            }
        }

        @Override
        public void onValueFromTextContent()
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onValueFromTextContent();
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onMultipleValues(KeyAttributeDefinitionPair[] definitions)
        {
            ManagedMap managedMap = new ManagedMap();
            for (KeyAttributeDefinitionPair definition : definitions)
            {
                ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
                definition.getAttributeDefinition().accept(valueExtractor);
                Object value = valueExtractor.getValue();
                if (value != null)
                {
                    managedMap.put(definition.getKey(), value);
                }
            }
            valueConsumer.accept(managedMap);
        }

    }

    /**
     * {code AttributeDefinitionVisitor} that extracts the value from the {@code ComponentModel}
     */
    private class ValueExtractorAttributeDefinitionVisitor implements AttributeDefinitionVisitor
    {
        private Object value;

        private String getStringValue()
        {
            return (String) getValue();
        }

        public Object getValue()
        {
            return value;
        }

        @Override
        public void onReferenceObject(Class<?> objectType)
        {
            objectReferencePopulator.populate(objectType, referenceId -> this.value = new RuntimeBeanReference(referenceId));
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            String reference = simpleParameters.get(configAttributeName);
            if (reference != null)
            {
                this.value = new RuntimeBeanReference(reference);
            }
            simpleParameters.remove(configAttributeName);
            if (configAttributeName.equals(PROCESSING_STRATEGY_ATTRIBUTE) || configAttributeName.equals("defaultProcessingStrategy"))
            {
                ProcessingStrategy processingStrategy = parseProcessingStrategy(reference);
                if (processingStrategy != null)
                {
                    this.value = processingStrategy;
                    return;
                }
            }
        }

        @Override
        public void onFixedValue(Object value)
        {
            this.value = value;
        }

        @Override
        public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter)
        {
            Object parameterValue = simpleParameters.get(parameterName);
            simpleParameters.remove(parameterName);
            parameterValue = parameterValue != null ? parameterValue : defaultValue;
            if (parameterValue != null)
            {
                parameterValue =  typeConverter.isPresent() ? typeConverter.get().convert(parameterValue) : parameterValue;
            }
            this.value = Optional.ofNullable(parameterValue).orElse(defaultValue);
        }

        @Override
        public void onUndefinedSimpleParameters()
        {
            this.value = simpleParameters;
        }

        @Override
        public void onUndefinedComplexParameters()
        {
            this.value = constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(complexParameters));
        }

        @Override
        public void onComplexChildCollection(Class<?> type, Optional<String> wrapperIdentifier)
        {
            Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, wrapperIdentifier);
            List<ComponentValue> matchingComponentValues = complexParameters.stream()
                    .filter(matchesTypeAndIdentifierPredicate)
                    .collect(toList());

            matchingComponentValues.stream().forEach(complexParameters::remove);
            if (wrapperIdentifier.isPresent() && !matchingComponentValues.isEmpty())
            {
                this.value = matchingComponentValues.get(0).getBean();
            }
            else
            {
                if (!matchingComponentValues.isEmpty())
                {
                    this.value = constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(matchingComponentValues));
                }
            }
        }

        @Override
        public void onComplexChildMap(Class<?> keyType, Class<?> valueType, String wrapperIdentifier)
        {
            complexParameters.stream()
                    .filter(getTypeAndIdentifierPredicate(MapFactoryBean.class, of(wrapperIdentifier)))
                    .findFirst()
                    .ifPresent(componentValue ->
                               {
                                   complexParameters.remove(componentValue);
                                   value = componentValue.getBean();
                               });
        }

        @Override
        public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier, Optional<String> childIdentifier)
        {
            Optional<String> identifier = wrapperIdentifier.isPresent() ? wrapperIdentifier : childIdentifier;
            Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, identifier);
            Optional<ComponentValue> value = complexParameters.stream().filter(matchesTypeAndIdentifierPredicate).findFirst();
            value.ifPresent(beanDefinitionTypePair -> {
                complexParameters.remove(beanDefinitionTypePair);
                Object bean = beanDefinitionTypePair.getBean();
                this.value = bean;
            });
        }

        private Predicate<ComponentValue> getTypeAndIdentifierPredicate(Class<?> type, Optional<String> identifierOptional)
        {
            return componentValue -> {
                        AtomicReference<Boolean> matchesIdentifier = new AtomicReference<>(true);
                        identifierOptional.ifPresent(wrapperIdentifier ->
                            matchesIdentifier.set(wrapperIdentifier.equals(componentValue.getComponentModel().getIdentifier().getName()))
                        );
                        return matchesIdentifier.get() && areMatchingTypes(type, componentValue.getType());
                    };
        }

        @Override
        public void onValueFromTextContent()
        {
            this.value = componentModel.getTextContent();
        }

        @Override
        public void onMultipleValues(KeyAttributeDefinitionPair[] definitions)
        {
            for (KeyAttributeDefinitionPair definition : definitions)
            {
                definition.getAttributeDefinition().accept(this);
            }
        }
    }

    private ManagedList constructManagedList(List<Object> beans)
    {
        ManagedList managedList = new ManagedList();
        managedList.addAll(beans);
        return managedList;
    }

    private List<Object> fromBeanDefinitionTypePairToBeanDefinition(List<ComponentValue> undefinedComplexParameters)
    {
        return undefinedComplexParameters.stream().map(ComponentValue::getBean).collect(toList());
    }

}

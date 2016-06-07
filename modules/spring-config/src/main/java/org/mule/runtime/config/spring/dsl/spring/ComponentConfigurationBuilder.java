/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSING_STRATEGY_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.spring.util.ProcessingStrategyUtils.parseProcessingStrategy;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.AttributeDefinitionVisitor;
import org.mule.runtime.core.api.MuleRuntimeException;
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
        componentBuildingDefinition.getIgnoredConfigurationParameters().stream().forEach( ignoredParameter -> {
            simpleParameters.remove(ignoredParameter);
        });
        for (Map.Entry<String, AttributeDefinition> definitionEntry : componentBuildingDefinition.getSetterParameterDefinitions().entrySet())
        {
            definitionEntry.getValue().accept(setterVisitor(definitionEntry.getKey()));
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
            return new ComponentValue(cdm.getIdentifier(), beanDefinitionType, bean);
        }).filter(beanDefinitionTypePair -> {
            return beanDefinitionTypePair != null;
        }).collect(toList());
    }

    private ConfigurableAttributeDefinitionVisitor constructorVisitor()
    {
        return new ConfigurableAttributeDefinitionVisitor(beanDefinitionBuilderHelper::addConstructorValue, beanDefinitionBuilderHelper::addConstructorReference);
    }

    private ConfigurableAttributeDefinitionVisitor setterVisitor(String propertyName)
    {
        return new ConfigurableAttributeDefinitionVisitor(beanDefinitionBuilderHelper.forProperty(propertyName)::addValue, beanDefinitionBuilderHelper.forProperty(propertyName)::addReference);
    }

    /**
     * Process a single {@link AttributeDefinition} from a {@link ComponentBuildingDefinition} and
     * uses an invokes a {@code Consumer} when the value is a bean definition or a different {@code Consumer}
     * if the value is a bean reference.
     */
    private class ConfigurableAttributeDefinitionVisitor implements AttributeDefinitionVisitor
    {

        private final Consumer<String> referenceConsumer;
        private final Consumer<Object> valueConsumer;

        /**
         * @param valueConsumer consumer for handling a bean definition
         * @param referenceConsumer consumer for handling a bean reference
         */
        ConfigurableAttributeDefinitionVisitor(Consumer<Object> valueConsumer, Consumer<String> referenceConsumer)
        {
            this.valueConsumer = valueConsumer;
            this.referenceConsumer = referenceConsumer;
        }

        @Override
        public void onReferenceObject(Class<?> objectType)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onReferenceObject(objectType);
            referenceConsumer.accept(valueExtractor.getStringValue());
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onReferenceSimpleParameter(configAttributeName);
            Object value = valueExtractor.getValue();
            if (value instanceof String)
            {
                referenceConsumer.accept((String) value);

            }
            else if (value != null)
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
        public void onComplexChildList(Class<?> type, Optional<String> wrapperIdentifier)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onComplexChildList(type, wrapperIdentifier);
            valueConsumer.accept(valueExtractor.getValue());
        }

        @Override
        public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier)
        {
            ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
            valueExtractor.onComplexChild(type, wrapperIdentifier);
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
        public void onMultipleValues(AttributeDefinition[] definitions)
        {
            ManagedMap managedMap = new ManagedMap();
            for (AttributeDefinition definition : definitions)
            {
                ValueExtractorAttributeDefinitionVisitor valueExtractor = new ValueExtractorAttributeDefinitionVisitor();
                KeyExtractorAttributeDefinitionVisitor keyExtractor = new KeyExtractorAttributeDefinitionVisitor();
                definition.accept(keyExtractor);
                definition.accept(valueExtractor);
                Object value = valueExtractor.getValue();
                if (value != null)
                {
                    managedMap.put(keyExtractor.getKey(), value);
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
            objectReferencePopulator.populate(objectType, (referenceId) -> {
                this.value = referenceId;
            });
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            String reference = simpleParameters.get(configAttributeName);
            this.value = reference;
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
            if (parameterValue != null)
            {
                parameterValue = typeConverter.isPresent() ? typeConverter.get().convert(parameterValue) : parameterValue;
            }
            simpleParameters.remove(parameterName);
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
        public void onComplexChildList(Class<?> type, Optional<String> wrapperIdentifier)
        {
            Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, wrapperIdentifier);
            List<ComponentValue> matchingComponentValues = complexParameters.stream()
                    .filter(matchesTypeAndIdentifierPredicate)
                    .collect(toList());

            matchingComponentValues.stream().forEach(beanDefinitionTypePair -> {
                complexParameters.remove(beanDefinitionTypePair);
            });
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
        public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier)
        {
            Predicate<ComponentValue> matchesTypeAndIdentifierPredicate = getTypeAndIdentifierPredicate(type, wrapperIdentifier);
            Optional<ComponentValue> value = complexParameters.stream().filter(matchesTypeAndIdentifierPredicate).findFirst();
            value.ifPresent(beanDefinitionTypePair -> {
                complexParameters.remove(beanDefinitionTypePair);
                Object bean = beanDefinitionTypePair.getBean();
                this.value = bean;
            });
        }

        private Predicate<ComponentValue> getTypeAndIdentifierPredicate(Class<?> type, Optional<String> identifierOptional)
        {
            return beanDefinitionTypePair -> {
                        AtomicReference<Boolean> matchesIdentifier = new AtomicReference<>(true);
                        identifierOptional.ifPresent(identifier -> {
                            matchesIdentifier.set(identifier.equals(beanDefinitionTypePair.getIdentifier().getName()));
                        });
                        return areMatchingTypes(type, beanDefinitionTypePair.getType()) && matchesIdentifier.get();
                    };
        }

        @Override
        public void onValueFromTextContent()
        {
            this.value = componentModel.getTextContent();
        }

        @Override
        public void onMultipleValues(AttributeDefinition[] definitions)
        {
            for (AttributeDefinition definition : definitions)
            {
                definition.accept(this);
            }
        }
    }

    /**
     * {code AttributeDefinitionVisitor} that generates a key from an {@code AttributeDefinition} to be used when
     * {@link AttributeDefinition.Builder#fromMultipleDefinitions(org.mule.runtime.config.spring.dsl.api.AttributeDefinition...)}
     * is used to define an object attribute. Such attribute will receive a {@code Map} with several configuration values
     * and the keys will be generated using this visitor.
     */
    private class KeyExtractorAttributeDefinitionVisitor implements AttributeDefinitionVisitor
    {
        private Object key;

        public Object getKey()
        {
            return key;
        }

        @Override
        public void onReferenceObject(Class<?> objectType)
        {
            this.key = objectType;
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            this.key = configAttributeName;
        }

        @Override
        public void onFixedValue(Object value)
        {
            this.key = value;
        }

        @Override
        public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter)
        {
            this.key = parameterName;
        }

        @Override
        public void onUndefinedSimpleParameters()
        {
            throw new MuleRuntimeException(createStaticMessage("onUndefinedSimpleParameters is not supported for multiple configuration attributes injection"));
        }

        @Override
        public void onUndefinedComplexParameters()
        {
            throw new MuleRuntimeException(createStaticMessage("onUndefinedComplexParameters is not supported for multiple configuration attributes injection"));
        }

        @Override
        public void onComplexChildList(Class<?> type, Optional<String> wrapperIdentifier)
        {
            this.key = type;
            wrapperIdentifier.ifPresent( (identifier) -> {
                this.key = identifier;
            });
        }

        @Override
        public void onComplexChild(Class<?> type, Optional<String> wrapperIdentifier)
        {
            onComplexChildList(type, wrapperIdentifier);
        }

        @Override
        public void onValueFromTextContent()
        {
            this.key = "context";
        }

        @Override
        public void onMultipleValues(AttributeDefinition[] definitions)
        {
            for (AttributeDefinition definition : definitions)
            {
                definition.accept(this);
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
        return undefinedComplexParameters.stream().map(beanDefinitionTypePair -> {
            return beanDefinitionTypePair.getBean();
        }).collect(toList());
    }

}

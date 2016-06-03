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

import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.TypeConverter;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.AttributeDefinitionVisitor;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.util.ClassUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.ManagedList;

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
    private final List<BeanValueTypePair> complexParameters;
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

    private List<BeanValueTypePair> collectComplexParametersWithTypes(ComponentModel componentModel)
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
            return new BeanValueTypePair(beanDefinitionType, bean);
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

    public class ConfigurableAttributeDefinitionVisitor implements AttributeDefinitionVisitor
    {

        private final Consumer<String> referencePopulator;
        private final Consumer<Object> valuePopulator;

        ConfigurableAttributeDefinitionVisitor(Consumer<Object> valuePopulator, Consumer<String> referencePopulator)
        {
            this.valuePopulator = valuePopulator;
            this.referencePopulator = referencePopulator;
        }

        @Override
        public void onReferenceObject(Class<?> objectType)
        {
            objectReferencePopulator.populate(objectType, referencePopulator);
        }

        @Override
        public void onReferenceSimpleParameter(final String configAttributeName)
        {
            String reference = simpleParameters.get(configAttributeName);
            if (configAttributeName.equals(PROCESSING_STRATEGY_ATTRIBUTE))
            {
                ProcessingStrategy processingStrategy = parseProcessingStrategy(reference);
                if (processingStrategy != null)
                {
                    valuePopulator.accept(processingStrategy);
                    return;
                }
            }
            simpleParameters.remove(configAttributeName);
            if (reference != null)
            {
                referencePopulator.accept(reference);
            }
            else
            {
                valuePopulator.accept(null);
            }
        }

        @Override
        public void onFixedValue(Object value)
        {
            valuePopulator.accept(value);
        }

        @Override
        public void onConfigurationParameter(String parameterName, Object defaultValue, Optional<TypeConverter> typeConverter)
        {
            Object value = simpleParameters.get(parameterName);
            if (value != null)
            {
                value = typeConverter.isPresent() ? typeConverter.get().convert(value) : value;
            }
            valuePopulator.accept(Optional.ofNullable(value).orElse(defaultValue));
            simpleParameters.remove(parameterName);
        }

        @Override
        public void onUndefinedSimpleParameters()
        {
            valuePopulator.accept(simpleParameters);
        }

        @Override
        public void onUndefinedComplexParameters()
        {
            valuePopulator.accept(constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(complexParameters)));
        }

        @Override
        public void onComplexChildList(Class<?> type)
        {
            List<BeanValueTypePair> matchingBeanValueTypePairs = complexParameters.stream().filter(beanDefinitionTypePair -> {
                return areMatchingTypes(type, beanDefinitionTypePair.getType());
            }).collect(toList());

            matchingBeanValueTypePairs.stream().forEach(beanDefinitionTypePair -> {
                complexParameters.remove(beanDefinitionTypePair);
            });

            valuePopulator.accept(constructManagedList(fromBeanDefinitionTypePairToBeanDefinition(matchingBeanValueTypePairs)));
        }

        @Override
        public void onComplexChild(Class<?> type)
        {
            Optional<BeanValueTypePair> value = complexParameters.stream().filter(beanDefinitionTypePair -> {
                return areMatchingTypes(type, beanDefinitionTypePair.getType());
            }).findFirst();
            value.ifPresent(beanDefinitionTypePair -> {
                complexParameters.remove(beanDefinitionTypePair);
                valuePopulator.accept(beanDefinitionTypePair.getBean());
            });
        }

        @Override
        public void onValueFromTextContent()
        {
            valuePopulator.accept(componentModel.getTextContent());
        }

    }

    private ManagedList constructManagedList(List<Object> beans)
    {
        ManagedList managedList = new ManagedList();
        managedList.addAll(beans);
        return managedList;
    }

    private List<Object> fromBeanDefinitionTypePairToBeanDefinition(List<BeanValueTypePair> undefinedComplexParameters)
    {
        return undefinedComplexParameters.stream().map(beanDefinitionTypePair -> {
            return beanDefinitionTypePair.getBean();
        }).collect(toList());
    }

}

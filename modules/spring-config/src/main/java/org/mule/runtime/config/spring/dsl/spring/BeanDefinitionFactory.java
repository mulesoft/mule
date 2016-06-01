/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.DESCRIPTION_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_ROOT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.NAME_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.QUEUE_STORE;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.adaptFilterBeanDefinitions;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.AbstractAttributeDefinitionVisitor;
import org.mule.runtime.core.api.MuleRuntimeException;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.w3c.dom.Element;

/**
 * The {@code BeanDefinitionFactory} is the one that knows how to convert a {@code ComponentModel} to an actual
 * {@link org.springframework.beans.factory.config.BeanDefinition} that can later be converted to a runtime object
 * that will be part of the artifact.
 *
 * It will recursively process a {@code ComponentModel} to create a {@code BeanDefinition}. For the time being
 * it will collaborate with the old bean definitions parsers for configurations that are partially defined in the
 * new parsing method.
 *
 * @since 4.0
 */
public class BeanDefinitionFactory
{

    private final ImmutableSet<ComponentIdentifier> ignoredMuleCoreComponentIdentifiers = ImmutableSet.<ComponentIdentifier>builder()
            .add(new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(MULE_ROOT_ELEMENT).build())
            .add(new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(DESCRIPTION_ELEMENT).build())
            .build();

    /**
     * These are the set of current language construct that have specific bean definitions parsers since we don't want to
     * include them in the parsing API.
     */
    private final ImmutableSet<ComponentIdentifier> customBuildersComponentIdentifiers = ImmutableSet.<ComponentIdentifier>builder()
            .add(new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(QUEUE_STORE).build())
            .build();


    private ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
    private BeanDefinitionCreator componentModelProcessor;
    private enum ChildType
    {
        SINGLE, COLLECTION
    }

    /**
     * @param componentBuildingDefinitionRegistry a registry with all the known {@code ComponentBuildingDefinition}s by the artifact.
     */
    public BeanDefinitionFactory(ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry)
    {
        this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
        this.componentModelProcessor = buildComponentModelProcessorChainOfResponsability();
    }

    /**
     * Creates a {@code BeanDefinition} by traversing the {@code ComponentModel} and its children.
     *
     * @param parentComponentModel the parent component model since the bean definition to be created may depend on the context.
     * @param componentModel the component model from which we want to create the bean definition.
     * @param registry the bean registry since it may be required to get other bean definitions to create this one or to register the bean definition.
     * @param componentModelPostProcessor a function to post process the bean definition.
     * @param oldParsingMechanism a function to execute the old parsing mechanism if required by children {@code ComponentModel}s
     * @return the {@code BeanDefinition} of the component model.
     */
    public BeanDefinition resolveComponentRecursively(ComponentModel parentComponentModel,
                                                      ComponentModel componentModel,
                                                      BeanDefinitionRegistry registry,
                                                      BiConsumer<ComponentModel, BeanDefinitionRegistry> componentModelPostProcessor,
                                                      BiFunction<Element, BeanDefinition, BeanDefinition> oldParsingMechanism)
    {
        List<ComponentModel> innerComponents = componentModel.getInnerComponents();
        if (!innerComponents.isEmpty())
        {
            for (ComponentModel innerComponent : innerComponents)
            {
                if (hasDefinition(innerComponent.getIdentifier(), of(innerComponent.getParent().getIdentifier())))
                {
                    resolveComponentRecursively(componentModel, innerComponent, registry, componentModelPostProcessor, oldParsingMechanism);
                }
                else
                {
                    AbstractBeanDefinition oldBeanDefinition = (AbstractBeanDefinition) oldParsingMechanism.apply((Element) from(innerComponent).getNode(), null);
                    oldBeanDefinition = adaptFilterBeanDefinitions(componentModel, oldBeanDefinition);
                    innerComponent.setBeanDefinition(oldBeanDefinition);
                }
            }
        }
        return resolveComponent(parentComponentModel, componentModel, registry, componentModelPostProcessor);
    }

    private BeanDefinition resolveComponent(ComponentModel parentComponentModel, ComponentModel componentModel, BeanDefinitionRegistry registry, BiConsumer<ComponentModel, BeanDefinitionRegistry> componentDefinitionModelProcessor)
    {
        if (ignoredMuleCoreComponentIdentifiers.contains(componentModel.getIdentifier()))
        {
            return null;
        }
        resolveComponentBeanDefinition(parentComponentModel, componentModel);
        componentDefinitionModelProcessor.accept(componentModel, registry);
        return componentModel.getBeanDefinition();
    }


    private void resolveComponentBeanDefinition(ComponentModel parentComponentModel, ComponentModel componentModel)
    {
        if (isWrapperComponent(componentModel.getIdentifier(), of(parentComponentModel.getIdentifier())))
        {
            processComponentWrapper(componentModel);
        }
        else
        {
            ComponentBuildingDefinition componentBuildingDefinition = null;
            if (!customBuildersComponentIdentifiers.contains(componentModel.getIdentifier()))
            {
                componentBuildingDefinition = componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier()).orElseThrow(() -> {
                    return new MuleRuntimeException(createStaticMessage(format("No component building definition for element %s. It may be that there's a dependency " +
                                                                               "missing to the project that handle that extension.",
                                                                               componentModel.getIdentifier())));
                });
            }
            this.componentModelProcessor.processRequest(new CreateBeanDefinitionRequest(parentComponentModel, componentModel, componentBuildingDefinition));
        }
    }

    private void processComponentWrapper(ComponentModel componentModel)
    {
        componentModel.setType(componentModel.getInnerComponents().get(0).getType());
        ComponentBuildingDefinition parentBuildingDefinition = componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getParent().getIdentifier()).get();
        Map<String, ChildType> wrapperIdentifierAndTypeMap = getWrapperIdentifierAndTypeMap(parentBuildingDefinition);
        ChildType wrapperType = wrapperIdentifierAndTypeMap.get(componentModel.getIdentifier().getName());
        if (wrapperType.equals(ChildType.COLLECTION))
        {
            ManagedList<Object> managedList = new ManagedList<>();
            for (ComponentModel innerComponentModel : componentModel.getInnerComponents())
            {
                Object value = innerComponentModel.getBeanDefinition() != null ? innerComponentModel.getBeanDefinition() : innerComponentModel.getBeanReference();
                managedList.add(value);
            }
            componentModel.setBeanDefinition(genericBeanDefinition(ArrayList.class)
                                                     .addConstructorArgValue(managedList)
                                                     .getBeanDefinition());
        }
        else
        {
            componentModel.setBeanDefinition(componentModel.getInnerComponents().get(0).getBeanDefinition());
            componentModel.setBeanReference(componentModel.getInnerComponents().get(0).getBeanReference());
        }
    }

    public static void checkElementNameUnique(BeanDefinitionRegistry registry, Element element)
    {
        if (null != element.getAttributeNode(NAME_ATTRIBUTE))
        {
            String name = element.getAttribute(NAME_ATTRIBUTE);
            if (registry.containsBeanDefinition(name))
            {
                throw new IllegalArgumentException("A component named " + name + " already exists.");
            }
        }
    }

    private BeanDefinitionCreator buildComponentModelProcessorChainOfResponsability()
    {
        ExceptionStrategyRefBeanDefinitionCreator exceptionStrategyRefBeanDefinitionCreator = new ExceptionStrategyRefBeanDefinitionCreator();
        FilterReferenceBeanDefinitionCreator filterReferenceBeanDefinitionCreator = new FilterReferenceBeanDefinitionCreator();
        ReferenceBeanDefinitionCreator referenceBeanDefinitionCreator = new ReferenceBeanDefinitionCreator();
        CommonBeanDefinitionCreator commonComponentModelProcessor = new CommonBeanDefinitionCreator();
        exceptionStrategyRefBeanDefinitionCreator.setNext(exceptionStrategyRefBeanDefinitionCreator);
        exceptionStrategyRefBeanDefinitionCreator.setNext(filterReferenceBeanDefinitionCreator);
        filterReferenceBeanDefinitionCreator.setNext(referenceBeanDefinitionCreator);
        referenceBeanDefinitionCreator.setNext(commonComponentModelProcessor);
        return exceptionStrategyRefBeanDefinitionCreator;
    }

    /**
     * Used to collaborate with the bean definition parsers mechanism.
     * If {@code #hasDefinition} returns false, then the old mechanism must be used.
     *
     * @param componentIdentifier a {@code ComponentModel} identifier.
     * @param parentComponentModelOptional the {@code ComponentModel} parent identifier.
     * @return true if there's a {@code ComponentBuildingDefinition} for the specified configuration identifier, false if there's not.
     */
    public boolean hasDefinition(ComponentIdentifier componentIdentifier, Optional<ComponentIdentifier> parentComponentModelOptional)
    {
        return ignoredMuleCoreComponentIdentifiers.contains(componentIdentifier)
               || customBuildersComponentIdentifiers.contains(componentIdentifier)
               || componentBuildingDefinitionRegistry.getBuildingDefinition(componentIdentifier).isPresent()
                || isWrapperComponent(componentIdentifier, parentComponentModelOptional);
    }

    //TODO MULE-9638 this code will be removed and a cache will be implemented
    public boolean isWrapperComponent(ComponentIdentifier componentModel, Optional<ComponentIdentifier> parentComponentModelOptional)
    {
        if (!parentComponentModelOptional.isPresent())
        {
            return false;
        }
        Optional<ComponentBuildingDefinition> buildingDefinitionOptional = componentBuildingDefinitionRegistry.getBuildingDefinition(parentComponentModelOptional.get());
        if (!buildingDefinitionOptional.isPresent())
        {
            return false;
        }
        final Map<String, ChildType> wrapperIdentifierAndTypeMap = getWrapperIdentifierAndTypeMap(buildingDefinitionOptional.get());
        return wrapperIdentifierAndTypeMap.containsKey(componentModel.getName());
    }

    private Map<String, ChildType> getWrapperIdentifierAndTypeMap(ComponentBuildingDefinition buildingDefinition)
    {
        final Map<String, ChildType> wrapperIdentifierAndTypeMap = new HashMap<>();
        AbstractAttributeDefinitionVisitor wrapperIdentifiersCollector = new AbstractAttributeDefinitionVisitor()
        {
            @Override
            public void onComplexChildList(Class<?> type, Optional<String> identifierOptional)
            {
                identifierOptional.ifPresent( identifier -> {
                    wrapperIdentifierAndTypeMap.put(identifier, ChildType.COLLECTION);
                });
            }

            @Override
            public void onComplexChild(Class<?> type, Optional<String> identifierOptional)
            {
                identifierOptional.ifPresent(identifier -> {
                    wrapperIdentifierAndTypeMap.put(identifier, ChildType.SINGLE);
                });
            }

            @Override
            public void onMultipleValues(AttributeDefinition[] definitions)
            {
                for (AttributeDefinition attributeDefinition : definitions)
                {
                    attributeDefinition.accept(this);
                }
            }
        };
        Consumer<AttributeDefinition> collectWrappersConsumer = attributeDefinition -> {
            attributeDefinition.accept(wrapperIdentifiersCollector);
        };
        buildingDefinition.getSetterParameterDefinitions().values().stream().forEach(collectWrappersConsumer);
        buildingDefinition.getConstructorAttributeDefinition().stream().forEach(collectWrappersConsumer);
        return wrapperIdentifierAndTypeMap;
    }
}

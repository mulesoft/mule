/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.config.spring.dsl.processor.ApplicationConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An {@code ApplicationModel} holds a representation of all the artifact configuration using an abstract model
 * to represent any configuration option.
 *
 * This model is represented by a set of {@link org.mule.runtime.config.spring.dsl.model.ComponentModel}. Each {@code ComponentModel}
 * holds a piece of configuration and may have children {@code ComponentModel}s as defined in the artifact configuration.
 *
 * Once the set of {@code ComponentModel} gets created from the application {@link org.mule.runtime.config.spring.dsl.processor.ConfigFile}s
 * the {@code ApplicationModel} executes a set of common validations dictated by the configuration semantics.
 *
 * @since 4.0
 */
public class ApplicationModel
{

    //TODO MULE-9692 move this logic elsewhere. This are here just for the language rules and those should be processed elsewhere.
    public static final String MULE_ROOT_ELEMENT = "mule";
    public static final String MULE_DOMAIN_ROOT_ELEMENT = "mule-domain";
    public static final String DESCRIPTION_ELEMENT = "description";
    public static final String CHOICE_EXCEPTION_STRATEGY = "choice-exception-strategy";
    public static final String DEFAULT_EXCEPTION_STRATEGY = "default-exception-strategy";
    public static final String MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE = "maxRedeliveryAttempts";
    public static final String WHEN_CHOICE_ES_ATTRIBUTE = "when";
    public static final String EXCEPTION_STRATEGY_REFERENCE_ELEMENT = "exception-strategy";
    public static final String SPRING_NAMESPACE = "spring";
    public static final String SPRING_CONTEXT_NAMESPACE = "context";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String REFERENCE_ATTRIBUTE = "ref";
    public static final String PROCESSOR_REFERENCE_ELEMENT = "processor";
    public static final String TRANSFORMER_REFERENCE_ELEMENT = "transformer";
    public static final String FILTER_REFERENCE_ELEMENT = "filter";
    public static final String MESSAGE_FILTER_ELEMENT = "message-filter";
    public static final String ANNOTATION_ELEMENT = "annotations";
    public static final String FILTER_ELEMENT_SUFFIX = "-filter";
    public static final String PROCESSING_STRATEGY_ATTRIBUTE = "processingStrategy";
    public static final String QUEUE_STORE = "queue-store";

    //TODO MULE-9638 Remove once all bean definitions parsers where migrated
    public static final String TEST_NAMESPACE = "test";
    public static final String DB_NAMESPACE = "db";
    public static final String JAAS_NAMESPACE = "jaas";
    public static final String SPRING_SECURITY_NAMESPACE = "ss";
    public static final String MULE_SECURITY_NAMESPACE = "mule-ss";
    public static final String MULE_XML_NAMESPACE = "mulexml";
    public static final String PGP_NAMESPACE = "pgp";
    public static final String XSL_NAMESPACE = "xsl";
    public static final String TRANSPORT_NAMESPACE = "transports";
    public static final String JMS_NAMESPACE = "jms";
    public static final String VM_NAMESPACE = "vm";
    public static final String HTTP_NAMESPACE = "http";
    public static final String BATCH_NAMESPACE = "batch";

    public static final ComponentIdentifier CHOICE_EXCEPTION_STRATEGY_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(CHOICE_EXCEPTION_STRATEGY).build();
    public static final ComponentIdentifier EXCEPTION_STRATEGY_REFERENCE_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(EXCEPTION_STRATEGY_REFERENCE_ELEMENT).build();
    public static final ComponentIdentifier MULE_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(MULE_ROOT_ELEMENT).build();
    public static final ComponentIdentifier MULE_DOMAIN_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(MULE_DOMAIN_ROOT_ELEMENT).build();
    public static final ComponentIdentifier SPRING_PROPERTY_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(SPRING_NAMESPACE).withName(PROPERTY_ELEMENT).build();
    public static final ComponentIdentifier MULE_PROPERTY_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(PROPERTY_ELEMENT).build();
    public static final ComponentIdentifier ANNOTATIONS_ELEMENT_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(ANNOTATION_ELEMENT).build();
    public static final ComponentIdentifier MESSAGE_FILTER_ELEMENT_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(MESSAGE_FILTER_ELEMENT).build();
    public static final ComponentIdentifier DEFAULT_ES_ELEMENT_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(DEFAULT_EXCEPTION_STRATEGY).build();
    public static final ComponentIdentifier PROCESSOR_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(PROCESSOR_REFERENCE_ELEMENT).build();
    public static final ComponentIdentifier TRANSFORMER_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(TRANSFORMER_REFERENCE_ELEMENT).build();
    public static final ComponentIdentifier QUEUE_STORE_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(QUEUE_STORE).build();

    private static ImmutableSet<ComponentIdentifier> ignoredNameValidationComponentList = ImmutableSet.<ComponentIdentifier>builder()
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("flow-ref").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("alias").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("in-memory-store").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("password-encryption-strategy").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("custom-security-provider").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("custom-encryption-strategy").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("secret-key-encryption-strategy").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("import").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("string-to-byte-array-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("append-string-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_ROOT_ELEMENT).withName("security-manager").build())
            .add(new ComponentIdentifier.Builder().withNamespace(TEST_NAMESPACE).withName("queue").build())
            .add(new ComponentIdentifier.Builder().withNamespace(TEST_NAMESPACE).withName("invocation-counter").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DB_NAMESPACE).withName("data-type").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DB_NAMESPACE).withName("in-param").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DB_NAMESPACE).withName("out-param").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DB_NAMESPACE).withName("template-query-ref").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DB_NAMESPACE).withName("inout-param").build())
            .add(new ComponentIdentifier.Builder().withNamespace(JAAS_NAMESPACE).withName("password-encryption-strategy").build())
            .add(new ComponentIdentifier.Builder().withNamespace(JAAS_NAMESPACE).withName("security-provider").build())
            .add(new ComponentIdentifier.Builder().withNamespace(SPRING_NAMESPACE).withName("property").build())
            .add(new ComponentIdentifier.Builder().withNamespace(SPRING_NAMESPACE).withName("bean").build())
            .add(new ComponentIdentifier.Builder().withNamespace(SPRING_SECURITY_NAMESPACE).withName("user").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_SECURITY_NAMESPACE).withName("delegate-security-provider").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_SECURITY_NAMESPACE).withName("security-manager").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_XML_NAMESPACE).withName("xslt-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(MULE_XML_NAMESPACE).withName("alias").build())
            .add(new ComponentIdentifier.Builder().withNamespace(PGP_NAMESPACE).withName("security-provider").build())
            .add(new ComponentIdentifier.Builder().withNamespace(PGP_NAMESPACE).withName("keybased-encryption-strategy").build())
            .add(new ComponentIdentifier.Builder().withNamespace(XSL_NAMESPACE).withName("param").build())
            .add(new ComponentIdentifier.Builder().withNamespace(XSL_NAMESPACE).withName("attribute").build())
            .add(new ComponentIdentifier.Builder().withNamespace(XSL_NAMESPACE).withName("element").build())
            .add(new ComponentIdentifier.Builder().withNamespace(TRANSPORT_NAMESPACE).withName("inbound-endpoint").build())
            .add(new ComponentIdentifier.Builder().withNamespace(TRANSPORT_NAMESPACE).withName("outbound-endpoint").build())
            .add(new ComponentIdentifier.Builder().withNamespace(JMS_NAMESPACE).withName("inbound-endpoint").build())
            .add(new ComponentIdentifier.Builder().withNamespace(VM_NAMESPACE).withName("inbound-endpoint").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("inbound-endpoint").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("set-cookie").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("header").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("http-response-to-object-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("http-response-to-string-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("message-to-http-response-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(HTTP_NAMESPACE).withName("object-to-http-request-transformer").build())
            .add(new ComponentIdentifier.Builder().withNamespace(BATCH_NAMESPACE).withName("step").build())
            .add(new ComponentIdentifier.Builder().withNamespace(BATCH_NAMESPACE).withName("execute").build())
            .build();

    private List<ComponentModel> componentModels = new ArrayList<>();

    /**
     * Creates an {code ApplicationModel} from a {@link ApplicationConfig}.
     *
     * A set of validations are applied that may make creation fail.
     *
     * @param applicationConfig the mule artifact configuration content.
     * @throws Exception when the application configuration has semantic errors.
     */
    public ApplicationModel(ApplicationConfig applicationConfig) throws Exception
    {
        convertConfigFileToComponentModel(applicationConfig);
        validateModel();
    }

    /**
     * @param element element which was the source of the {@code ComponentModel}.
     * @return the {@code ComponentModel} created from the element.
     */
    //TODO MULE-9638: remove once the old parsing mechanism is not needed anymore
    public ComponentModel findComponentDefinitionModel(Element element)
    {
        return innerFindComponentDefinitionModel(element, componentModels);
    }

    private void convertConfigFileToComponentModel(ApplicationConfig applicationConfig)
    {
        List<ConfigFile> configFiles = applicationConfig.getConfigFiles();
        configFiles.stream()
                .filter(configFile -> {
                    if (configFile.getConfigLines().isEmpty())
                    {
                        return false;
                    }
                    return !isSpringFile(configFile);
                })
                .forEach(configFile -> {
                    componentModels.addAll(extractComponentDefinitionModel(Arrays.asList(configFile.getConfigLines().get(0)), configFile.getFilename()));
                });
    }

    private boolean isSpringFile(ConfigFile configFile)
    {
        return configFile.getConfigLines().get(0).getIdentifier().equals(ApplicationModel.SPRING_NAMESPACE);
    }

    private void validateModel() throws ConfigurationException
    {
        if (componentModels.isEmpty() || !isMuleConfigurationFile())
        {
            return;
        }
        validateNameIsNotRepeated();
        validateNameIsOnlyOnTopLevelElements();
        validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice();
        validateChoiceExceptionStrategyStructure();
    }

    private void validateNameIsNotRepeated()
    {
        Map<String, ComponentModel> existingObjectsWithName = new HashMap<>();
        executeOnEveryComponentTree(componentModel -> {
            String nameAttributeValue = componentModel.getNameAttribute();
            if (nameAttributeValue != null && !ignoredNameValidationComponentList.contains(componentModel.getIdentifier()))
            {
                if (existingObjectsWithName.containsKey(nameAttributeValue))
                {
                    throw new MuleRuntimeException(createStaticMessage("Two configuration elements have been defined with the same global name. Global name must be unique. Clashing components are %s and %s",
                                                                       existingObjectsWithName.get(nameAttributeValue).getIdentifier(),
                                                                       componentModel.getIdentifier()));
                }
                existingObjectsWithName.put(nameAttributeValue, componentModel);
            }
        });
    }

    private boolean isMuleConfigurationFile()
    {
        return componentModels.get(0).getIdentifier().equals(MULE_IDENTIFIER);
    }

    private void validateChoiceExceptionStrategyStructure()
    {
        executeOnEveryComponentTree(component -> {
            if (component.getIdentifier().equals(CHOICE_EXCEPTION_STRATEGY_IDENTIFIER))
            {
                validateExceptionStrategiesHaveWhenAttribute(component);
                validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(component);
            }
        });
    }

    private void validateNoMoreThanOneRollbackExceptionStrategyWithRedelivery(ComponentModel component)
    {
        if (component.getInnerComponents().stream().filter(exceptionStrategyComponent -> {
            return exceptionStrategyComponent.getParameters().get(MAX_REDELIVERY_ATTEMPTS_ROLLBACK_ES_ATTRIBUTE) != null;
        }).count() > 1)
        {
            throw new MuleRuntimeException(createStaticMessage("Only one rollback-exception-strategy within a choice-exception-strategy can handle message redelivery. Remove one of the maxRedeliveryAttempts attributes"));
        }
    }

    private void validateExceptionStrategiesHaveWhenAttribute(ComponentModel component)
    {
        List<ComponentModel> innerComponents = component.getInnerComponents();
        for (int i = 0; i < innerComponents.size() - 1; i++)
        {
            if (innerComponents.get(i).getParameters().get(WHEN_CHOICE_ES_ATTRIBUTE) == null)
            {
                throw new MuleRuntimeException(createStaticMessage("Every exception strategy (except for the last one) within a choice-exception-strategy must specify the when attribute"));
            }
        }
    }

    private void validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice()
    {
        executeOnEveryComponentTree(component -> {
            if (component.getIdentifier().getName().endsWith(EXCEPTION_STRATEGY_REFERENCE_ELEMENT))
            {
                Node componentNode = from(component).getNode();
                if (component.getParameters().get(WHEN_CHOICE_ES_ATTRIBUTE) != null
                    && !componentNode.getParentNode().getLocalName().equals(CHOICE_EXCEPTION_STRATEGY)
                    && !componentNode.getParentNode().getLocalName().equals(MULE_ROOT_ELEMENT))
                {
                    throw new MuleRuntimeException(createStaticMessage("Only exception strategies within a choice-exception-strategy can have when attribute specified"));
                }
            }
        });
    }

    private void validateNameIsOnlyOnTopLevelElements() throws ConfigurationException
    {
        try
        {
            List<ComponentModel> topLevelComponents = componentModels.get(0).getInnerComponents();
            topLevelComponents.stream().filter(this::isMuleComponent).forEach(topLevelComponent -> {
                topLevelComponent.getInnerComponents().stream().filter(this::isMuleComponent).forEach((topLevelComponentChild -> {
                    executeOnComponentTree(topLevelComponentChild, (component) -> {
                        if (component.getNameAttribute() != null && !ignoredNameValidationComponentList.contains(component.getIdentifier()))
                        {
                            throw new MuleRuntimeException(createStaticMessage("Only top level elements can have a name attribute. Component %s has attribute name with value %s", component.getIdentifier(), component.getNameAttribute()));
                        }
                    });
                }));

            });
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    private boolean isMuleComponent(ComponentModel componentModel)
    {
        return !componentModel.getIdentifier().getNamespace().equals(ApplicationModel.SPRING_NAMESPACE);
    }

    private void executeOnEveryComponentTree(final ComponentConsumer task)
    {
        for (ComponentModel componentModel : componentModels)
        {
            executeOnComponentTree(componentModel, task);
        }
    }

    private void executeOnComponentTree(final ComponentModel component, final ComponentConsumer task) throws MuleRuntimeException
    {
        if (component.getIdentifier().getNamespace().equals(SPRING_NAMESPACE))
        {
            //TODO MULE-9648: for now do no process beans inside spring
            return;
        }
        component.getInnerComponents().forEach((innerComponent) -> {
            executeOnComponentTree(innerComponent, task);
        });
        task.consume(component);
    }

    private List<ComponentModel> extractComponentDefinitionModel(List<ConfigLine> configLines, String configFileName)
    {
        List<ComponentModel> models = new ArrayList<>();
        for (ConfigLine configLine : configLines)
        {
            String namespace = configLine.getNamespace() == null ? CORE_NAMESPACE_NAME : configLine.getNamespace();
            ComponentModel.Builder builder = new ComponentModel.Builder()
                    .setIdentifier(new ComponentIdentifier.Builder()
                                           .withNamespace(namespace)
                                           .withName(configLine.getIdentifier())
                                           .build())
                    .setTextContent(configLine.getTextContent());
            to(builder).addNode(from(configLine).getNode()).addConfigFileName(configFileName);
            for (SimpleConfigAttribute simpleConfigAttribute : configLine.getConfigAttributes().values())
            {
                builder.addParameter(simpleConfigAttribute.getName(), simpleConfigAttribute.getValue());
            }
            List<ComponentModel> componentModels = extractComponentDefinitionModel(configLine.getChildren(), configFileName);
            componentModels.stream().forEach(componentDefinitionModel -> {
                if (SPRING_PROPERTY_IDENTIFIER.equals(componentDefinitionModel.getIdentifier()))
                {
                    builder.addParameter(componentDefinitionModel.getNameAttribute(), componentDefinitionModel.getParameters().get("value"));
                }
                builder.addChildComponentModel(componentDefinitionModel);
            });
            ConfigLine parent = configLine.getParent();
            if (parent != null && isConfigurationTopComponent(parent))
            {
                builder.markAsRootComponent();
            }
            ComponentModel componentModel = builder.build();
            for (ComponentModel innerComponentModel : componentModel.getInnerComponents())
            {
                innerComponentModel.setParent(componentModel);
            }
            models.add(componentModel);
        }
        return models;
    }

    private boolean isConfigurationTopComponent(ConfigLine parent)
    {
        return (parent.getIdentifier().equals(MULE_ROOT_ELEMENT) || parent.getIdentifier().equals(MULE_DOMAIN_ROOT_ELEMENT));
    }

    private ComponentModel innerFindComponentDefinitionModel(Element element, List<ComponentModel> componentModels)
    {
        for (ComponentModel componentModel : componentModels)
        {
            if (from(componentModel).getNode().equals(element))
            {
                return componentModel;
            }
            ComponentModel childComponentModel = innerFindComponentDefinitionModel(element, componentModel.getInnerComponents());
            if (childComponentModel != null)
            {
                return childComponentModel;
            }
        }
        return null;
    }

    /**
     * Functional interface to process a component.
     */
    @FunctionalInterface
    interface ComponentConsumer
    {

        void consume(ComponentModel componentModel) throws MuleRuntimeException;
    }
}

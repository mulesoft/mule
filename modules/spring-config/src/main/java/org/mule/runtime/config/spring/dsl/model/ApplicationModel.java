/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.lang.String.format;
import static java.lang.System.getProperties;
import static java.lang.System.getenv;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.to;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.api.util.NameUtils.pluralize;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.SimpleConfigAttribute;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.util.PropertyPlaceholderHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An {@code ApplicationModel} holds a representation of all the artifact configuration using an abstract model
 * to represent any configuration option.
 * <p/>
 * This model is represented by a set of {@link org.mule.runtime.config.spring.dsl.model.ComponentModel}. Each {@code ComponentModel}
 * holds a piece of configuration and may have children {@code ComponentModel}s as defined in the artifact configuration.
 * <p/>
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
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String PROCESSOR_REFERENCE_ELEMENT = "processor";
    public static final String TRANSFORMER_REFERENCE_ELEMENT = "transformer";
    public static final String FILTER_REFERENCE_ELEMENT = "filter";
    public static final String MESSAGE_FILTER_ELEMENT = "message-filter";
    public static final String ANNOTATION_ELEMENT = "annotations";
    public static final String FILTER_ELEMENT_SUFFIX = "-filter";
    public static final String PROCESSING_STRATEGY_ATTRIBUTE = "processingStrategy";
    public static final String QUEUE_STORE = "queue-store";
    public static final String CONFIGURATION_ELEMENT = "configuration";
    public static final String DATA_WEAVE = "weave";

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
    public static final String PARSER_TEST_NAMESPACE = "parsers-test";
    public static final String PROPERTY_PLACEHOLDER_ELEMENT = "property-placeholder";

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
    public static final ComponentIdentifier CONFIGURATION_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(CONFIGURATION_ELEMENT).build();
    public static final ComponentIdentifier SPRING_PROPERTY_PLACEHOLDER_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(SPRING_CONTEXT_NAMESPACE).withName(PROPERTY_PLACEHOLDER_ELEMENT).build();

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
            .add(new ComponentIdentifier.Builder().withNamespace(PARSER_TEST_NAMESPACE).withName("child").build())
            .add(new ComponentIdentifier.Builder().withNamespace(PARSER_TEST_NAMESPACE).withName("kid").build())
            .add(new ComponentIdentifier.Builder().withNamespace(DATA_WEAVE).withName("reader-property").build())
            .build();

    private List<ComponentModel> muleComponentModels = new LinkedList<>();
    private List<ComponentModel> springComponentModels = new LinkedList<>();
    private PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
    private Properties applicationProperties;

    /**
     * Creates an {code ApplicationModel} from a {@link ArtifactConfig}.
     * <p/>
     * A set of validations are applied that may make creation fail.
     *
     * @param artifactConfig the mule artifact configuration content.
     * @param artifactConfiguration
     * @throws Exception when the application configuration has semantic errors.
     */
    public ApplicationModel(ArtifactConfig artifactConfig, ArtifactConfiguration artifactConfiguration) throws Exception
    {
        configurePropertyPlaceholderResolver(artifactConfig);
        convertConfigFileToComponentModel(artifactConfig);
        convertArtifactConfigurationToComponentModel(artifactConfiguration);
        validateModel();
    }

    private void convertArtifactConfigurationToComponentModel(ArtifactConfiguration artifactConfiguration)
    {
        if (artifactConfiguration != null)
        {
            for (ComponentConfiguration componentConfiguration : artifactConfiguration.getComponentConfiguration())
            {
                ComponentModel componentModel = convertComponentConfiguration(componentConfiguration, true);
                this.muleComponentModels.add(componentModel);
            }
        }
    }

    private ComponentModel convertComponentConfiguration(ComponentConfiguration componentConfiguration, boolean isRoot)
    {
        ComponentModel.Builder builder = new ComponentModel.Builder()
                .setIdentifier(new ComponentIdentifier.Builder()
                                       .withName(componentConfiguration.getIdentifier())
                                       .withNamespace(componentConfiguration.getNamespace()).build());
        if (isRoot)
        {
            builder.markAsRootComponent();
        }
        for (Map.Entry<String, String> parameter : componentConfiguration.getParameters().entrySet())
        {
            builder.addParameter(parameter.getKey(), parameter.getValue(), false);
        }
        for (ComponentConfiguration childComponentConfiguration : componentConfiguration.getNestedComponentConfiguration())
        {
            builder.addChildComponentModel(convertComponentConfiguration(childComponentConfiguration, false));
        }
        return builder.build();

    }

    private void configurePropertyPlaceholderResolver(ArtifactConfig artifactConfig)
    {
        //TODO MULE-9825: a new mechanism for property placeholders need to be defined
        final List<String> locations = new ArrayList<>();
        artifactConfig.getConfigFiles().stream().forEach(configFile -> {
            configFile.getConfigLines().get(0).getChildren().stream().forEach( configLine -> {
                if (configLine.getIdentifier().equals(PROPERTY_PLACEHOLDER_ELEMENT))
                {
                    String locationValue = configLine.getConfigAttributes().get("location").getValue();
                    locationValue = propertyPlaceholderHelper.replacePlaceholders(locationValue, getProperties());
                    locationValue = locationValue.replace("classpath:/", "");
                    locations.add(locationValue);
                }
            });
        });
        applicationProperties = new Properties();
        applicationProperties.putAll(getenv());
        applicationProperties.putAll(getProperties());
        //TODO MULE-9638: This check should not be required once we don't use the old mechanism.
        if (artifactConfig.getApplicationProperties() != null)
        {
            applicationProperties.putAll(artifactConfig.getApplicationProperties());

        }
        for (String propertyFileLocation : locations)
        {
            Properties properties = new Properties();
            try (InputStream propertiesFileInputStream = currentThread().getContextClassLoader().getResourceAsStream(propertyFileLocation))
            {
                properties.load(propertiesFileInputStream);
                applicationProperties.putAll(properties);
            }
            catch (IOException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    /**
     * @param element element which was the source of the {@code ComponentModel}.
     * @return the {@code ComponentModel} created from the element.
     */
    //TODO MULE-9638: remove once the old parsing mechanism is not needed anymore
    public ComponentModel findComponentDefinitionModel(Element element)
    {
        return innerFindComponentDefinitionModel(element, muleComponentModels);
    }

    public Optional<ComponentModel> findComponentDefinitionModel(ComponentIdentifier componentIdentifier)
    {
        if (muleComponentModels.isEmpty())
        {
            return empty();
        }
        return muleComponentModels.get(0).getInnerComponents().stream()
                .filter(ComponentModel::isRoot)
                .filter(componentModel ->
                                componentModel.getIdentifier().equals(componentIdentifier)
                ).findFirst();
    }

    private void convertConfigFileToComponentModel(ArtifactConfig artifactConfig)
    {
        List<ConfigFile> configFiles = artifactConfig.getConfigFiles();
        configFiles.stream()
                .forEach(configFile -> {
                    List<ComponentModel> componentModels = extractComponentDefinitionModel(asList(configFile.getConfigLines().get(0)), configFile.getFilename());
                    if (isMuleConfigFile(configFile))
                    {
                        muleComponentModels.addAll(componentModels);
                    }
                    else
                    {
                        springComponentModels.addAll(componentModels);
                    }
                });

    }

    private boolean isMuleConfigFile(final ConfigFile configFile)
    {
        if (configFile.getConfigLines().isEmpty())
        {
            return false;
        }
        return !isSpringFile(configFile);
    }

    private boolean isSpringFile(ConfigFile configFile)
    {
        return SPRING_NAMESPACE.equals(configFile.getConfigLines().get(0).getNamespace());
    }

    public boolean hasSpringConfig()
    {
        return !springComponentModels.isEmpty();
    }

    private void validateModel() throws ConfigurationException
    {
        if (muleComponentModels.isEmpty() || !isMuleConfigurationFile())
        {
            return;
        }
        //TODO MULE-9692 all this validations will be moved to an entity that does the validation and allows to aggregate all validations instead of failing fast.
        validateNameIsNotRepeated();
        validateNameIsOnlyOnTopLevelElements();
        validateExceptionStrategyWhenAttributeIsOnlyPresentInsideChoice();
        validateChoiceExceptionStrategyStructure();
        validateNoDefaultExceptionStrategyAsGlobal();
        validateParameterAndChildForSameAttributeAreNotDefinedTogether();
    }

    private void validateParameterAndChildForSameAttributeAreNotDefinedTogether()
    {
        executeOnEveryMuleComponentTree(componentModel -> {
            for (String parameterName : componentModel.getParameters().keySet())
            {
                if (!componentModel.isParameterValueProvidedBySchema(parameterName))
                {
                    String mapChildName = hyphenize(pluralize(parameterName));
                    String listOrPojoChildName = hyphenize(parameterName);
                    Optional<ComponentModel> childOptional = findRelatedChildForParameter(componentModel.getInnerComponents(), mapChildName, listOrPojoChildName);
                    if (childOptional.isPresent() && !childOptional.get().getIdentifier().equals(SPRING_PROPERTY_IDENTIFIER))
                    {
                        throw new MuleRuntimeException(createStaticMessage(format("Component %s has a child element %s which is used for the same purpose of the configuration parameter %s. " +
                                                                                  "Only one must be used.", componentModel.getIdentifier(), childOptional.get().getIdentifier(), parameterName)));
                    }
                }
            }
        });
    }

    private Optional<ComponentModel> findRelatedChildForParameter(List<ComponentModel> chilrenComponents, String... possibleNames)
    {
        Set<String> possibleNamesSet = new HashSet<>(asList(possibleNames));
        for (ComponentModel childrenComponent : chilrenComponents)
        {
            if (possibleNamesSet.contains(childrenComponent.getIdentifier().getName()))
            {
                return of(childrenComponent);
            }
        }
        return empty();
    }

    private void validateNoDefaultExceptionStrategyAsGlobal()
    {
        executeOnEveryMuleComponentTree(componentModel -> {
            if (componentModel.isRoot() && DEFAULT_ES_ELEMENT_IDENTIFIER.equals(componentModel.getIdentifier()))
            {
                if (componentModel.getNameAttribute() != null)
                {
                    throw new MuleRuntimeException(createStaticMessage(format("Component %s is not supported as global", DEFAULT_ES_ELEMENT_IDENTIFIER.getName())));
                }
            }
        });
    }

    private void validateNameIsNotRepeated()
    {
        Map<String, ComponentModel> existingObjectsWithName = new HashMap<>();
        executeOnEveryMuleComponentTree(componentModel -> {
            String nameAttributeValue = componentModel.getNameAttribute();
            if (nameAttributeValue != null && !ignoredNameValidationComponentList.contains(componentModel.getIdentifier()))
            {
                if (existingObjectsWithName.containsKey(nameAttributeValue))
                {
                    throw new MuleRuntimeException(createStaticMessage("Two configuration elements have been defined with the same global name. Global name [%s] must be unique. Clashing components are %s and %s",
                                                                       nameAttributeValue,
                                                                       existingObjectsWithName.get(nameAttributeValue).getIdentifier(),
                                                                       componentModel.getIdentifier()));
                }
                existingObjectsWithName.put(nameAttributeValue, componentModel);
            }
        });
    }

    private boolean isMuleConfigurationFile()
    {
        return muleComponentModels.get(0).getIdentifier().equals(MULE_IDENTIFIER);
    }

    private void validateChoiceExceptionStrategyStructure()
    {
        executeOnEveryMuleComponentTree(component -> {
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
        executeOnEveryMuleComponentTree(component -> {
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
            List<ComponentModel> topLevelComponents = muleComponentModels.get(0).getInnerComponents();
            topLevelComponents.stream().filter(this::isMuleComponent).forEach(topLevelComponent -> {
                topLevelComponent.getInnerComponents().stream().filter(this::isMuleComponent).forEach((topLevelComponentChild -> {
                    executeOnComponentTree(topLevelComponentChild, (component) -> {
                        if (component.getNameAttribute() != null && !ignoredNameValidationComponentList.contains(component.getIdentifier()))
                        {
                            throw new MuleRuntimeException(createStaticMessage("Only top level elements can have a name attribute. Component %s has attribute name with value %s", component.getIdentifier(), component.getNameAttribute()));
                        }
                    }, true);
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

    public void executeOnEveryComponentTree(final Consumer<ComponentModel> task)
    {
        for (ComponentModel componentModel : muleComponentModels)
        {
            executeOnComponentTree(componentModel, task, false);
        }
    }

    public void executeOnEveryMuleComponentTree(final Consumer<ComponentModel> task)
    {
        for (ComponentModel componentModel : muleComponentModels)
        {
            executeOnComponentTree(componentModel, task, true);
        }
    }

    private void executeOnComponentTree(final ComponentModel component, final Consumer<ComponentModel> task, boolean avoidSpringElements) throws MuleRuntimeException
    {
        if (component.getIdentifier().getNamespace().equals(SPRING_NAMESPACE) && avoidSpringElements)
        {
            //TODO MULE-9648: for now do no process beans inside spring
            return;
        }
        component.getInnerComponents().forEach((innerComponent) -> {
            executeOnComponentTree(innerComponent, task, avoidSpringElements);
        });
        task.accept(component);
    }

    private List<ComponentModel> extractComponentDefinitionModel(List<ConfigLine> configLines, String configFileName)
    {
        List<ComponentModel> models = new ArrayList<>();
        for (final ConfigLine configLine : configLines)
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
                builder.addParameter(simpleConfigAttribute.getName(), resolveValueIfIsPlaceHolder(simpleConfigAttribute.getValue()), simpleConfigAttribute.isValueFromSchema());
            }
            List<ComponentModel> componentModels = extractComponentDefinitionModel(configLine.getChildren(), configFileName);
            componentModels.stream().forEach(componentDefinitionModel -> {
                if (SPRING_PROPERTY_IDENTIFIER.equals(componentDefinitionModel.getIdentifier()))
                {
                    String value = componentDefinitionModel.getParameters().get(VALUE_ATTRIBUTE);
                    if (value != null)
                    {
                        builder.addParameter(componentDefinitionModel.getNameAttribute(), resolveValueIfIsPlaceHolder(value), false);
                    }
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

    private String resolveValueIfIsPlaceHolder(String value)
    {
        return propertyPlaceholderHelper.replacePlaceholders(value, applicationProperties);
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
     * TODO MULE-9688: When the model it's made immutable we will also provide the parent component for navigation and this will not be needed anymore.
     * @return the root component model
     */
    public ComponentModel getRootComponentModel()
    {
        return muleComponentModels.get(0);
    }

}

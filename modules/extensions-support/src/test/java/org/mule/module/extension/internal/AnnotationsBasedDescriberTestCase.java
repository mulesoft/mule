/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.extension.api.introspection.ExpressionSupport.REQUIRED;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.module.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.module.extension.HeisenbergExtension.AGE;
import static org.mule.module.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.module.extension.HeisenbergExtension.PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.module.extension.HeisenbergExtension.PARAMETER_OVERRIDED_DISPLAY_NAME;
import static org.mule.module.extension.HeisenbergExtension.PERSONAL_INFORMATION_GROUP_NAME;
import static org.mule.module.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import static org.mule.module.extension.HeisenbergExtension.SCHEMA_VERSION;
import static org.mule.module.extension.HeisenbergOperations.KILL_WITH_GROUP;
import static org.mule.module.extension.HeisenbergOperations.OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME;
import static org.mule.module.extension.HeisenbergOperations.OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME;
import static org.mule.module.extension.internal.ExtensionProperties.TLS_ATTRIBUTE_NAME;
import static org.mule.module.extension.internal.introspection.describer.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.arrayOf;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.objectTypeBuilder;
import static org.mule.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.api.MuleEvent;
import org.mule.api.temporary.MuleMessage;
import org.mule.api.tls.TlsContextFactory;
import org.mule.config.MuleManifest;
import org.mule.extension.api.annotation.Configuration;
import org.mule.extension.api.annotation.Configurations;
import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Parameter;
import org.mule.extension.api.annotation.ParameterGroup;
import org.mule.extension.api.annotation.Sources;
import org.mule.extension.api.annotation.capability.Xml;
import org.mule.extension.api.annotation.connector.Providers;
import org.mule.extension.api.annotation.param.Optional;
import org.mule.extension.api.annotation.param.UseConfig;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.extension.api.introspection.property.DisplayModelProperty;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.java.annotation.GenericTypesAnnotation;
import org.mule.module.extension.HeisenbergConnection;
import org.mule.module.extension.HeisenbergConnectionProvider;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.HeisenbergSource;
import org.mule.module.extension.MoneyLaunderingOperation;
import org.mule.module.extension.exception.CureCancerExceptionEnricher;
import org.mule.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.module.extension.internal.exception.IllegalOperationModelDefinitionException;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.module.extension.model.ExtendedPersonalInfo;
import org.mule.module.extension.model.HealthStatus;
import org.mule.module.extension.model.KnockeableDoor;
import org.mule.module.extension.model.Ricin;
import org.mule.module.extension.model.Weapon;
import org.mule.tck.size.SmallTest;
import org.mule.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AnnotationsBasedDescriberTestCase extends AbstractAnnotationsBasedDescriberTestCase
{

    private static final String EXTENDED_CONFIG_NAME = "extendedConfig";
    private static final String EXTENDED_CONFIG_DESCRIPTION = "extendedDescription";

    private static final String SOURCE_NAME = "ListenPayments";
    private static final String SOURCE_PARAMETER = "initialBatchNumber";

    private static final String SAY_MY_NAME_OPERATION = "sayMyName";
    private static final String GET_ENEMY_OPERATION = "getEnemy";
    private static final String KILL_OPERATION = "kill";
    private static final String KILL_CUSTOM_OPERATION = "killWithCustomMessage";
    private static final String KILL_WITH_WEAPON = "killWithWeapon";
    private static final String KILL_WITH_MULTIPLES_WEAPONS = "killWithMultiplesWeapons";
    private static final String KILL_WITH_MULTIPLE_WILDCARD_WEAPONS = "killWithMultipleWildCardWeapons";
    private static final String GET_PAYMENT_FROM_EVENT_OPERATION = "getPaymentFromEvent";
    private static final String GET_PAYMENT_FROM_MESSAGE_OPERATION = "getPaymentFromMessage";
    private static final String DIE = "die";
    private static final String KILL_MANY = "killMany";
    private static final String KILL_ONE = "killOne";
    private static final String LAUNDER_MONEY = "launder";
    private static final String INJECTED_EXTENSION_MANAGER = "getInjectedExtensionManager";
    private static final String ALIAS = "alias";
    private static final String KNOCK = "knock";
    private static final String KNOCK_MANY = "knockMany";
    private static final String CALL_SAUL = "callSaul";
    private static final String CALL_GUS_FRING = "callGusFring";
    private static final String CURE_CANCER = "cureCancer";
    private static final String GET_SAUL_PHONE = "getSaulPhone";
    private static final String IGNORED_OPERATION = "ignoredOperation";

    private static final String EXTENSION_VERSION = MuleManifest.getProductVersion();
    private static final String PARAMETER_GROUP_DISPLAY_NAME = "Date of decease";
    private static final String PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME = "dateOfDeath";

    @Before
    public void setUp()
    {
        setDescriber(describerFor(HeisenbergExtension.class));
    }

    @Test
    public void describeTestModule() throws Exception
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());

        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        assertExtensionProperties(declaration);

        assertTestModuleConfiguration(declaration);
        assertTestModuleOperations(declaration);
        assertTestModuleConnectionProviders(declaration);
        assertTestModuleMessageSource(declaration);
        assertModelProperties(declaration);
    }

    @Test
    public void parseDisplayAnnotationsOnParameter()
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        List<ParameterDeclaration> parameters = declaration.getConfigurations().get(0).getParameters();

        assertParameterPlacement(findParameter(parameters, "labeledRicin"), RICIN_GROUP_NAME, 1);
        assertParameterPlacement(findParameter(parameters, "ricinPacks"), RICIN_GROUP_NAME, 2);

        assertParameterPlacement(findParameter(parameters, "ricinPacks"), RICIN_GROUP_NAME, 2);
        assertParameterDisplayName(findParameter(parameters, PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME), PARAMETER_OVERRIDED_DISPLAY_NAME);
    }

    @Test
    public void parseDisplayAnnotationsOnParameterGroup()
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        List<ParameterDeclaration> parameters = declaration.getConfigurations().get(0).getParameters();

        assertParameterPlacement(findParameter(parameters, "dateOfBirth"), PERSONAL_INFORMATION_GROUP_NAME, null);
        assertParameterPlacement(findParameter(parameters, "dateOfDeath"), PERSONAL_INFORMATION_GROUP_NAME, null);
        assertParameterPlacement(findParameter(parameters, "age"), PERSONAL_INFORMATION_GROUP_NAME, null);
        assertParameterPlacement(findParameter(parameters, "myName"), PERSONAL_INFORMATION_GROUP_NAME, null);
    }

    @Test
    public void parseDisplayNameAnnotationOnParameterGroup()
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        List<ParameterDeclaration> parameters = declaration.getConfigurations().get(0).getParameters();

        assertParameterDisplayName(findParameter(parameters, PARAMETER_GROUP_ORIGINAL_DISPLAY_NAME), PARAMETER_GROUP_DISPLAY_NAME);
    }

    @Test
    public void parseDisplayNameAnnotationOnOperationParameter()
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        OperationDeclaration operation = getOperation(declaration, HeisenbergOperations.OPERATION_WITH_DISPLAY_NAME_PARAMETER);

        assertThat(operation, is(notNullValue()));
        List<ParameterDeclaration> parameters = operation.getParameters();

        assertParameterDisplayName(findParameter(parameters, OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME), OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME);
    }

    @Test
    public void parseDisplayAnnotationsOnOperationParameter()
    {
        Descriptor descriptor = getDescriber().describe(new DefaultDescribingContext());
        Declaration declaration = descriptor.getRootDeclaration().getDeclaration();
        OperationDeclaration operation = getOperation(declaration, KILL_CUSTOM_OPERATION);

        assertThat(operation, is(notNullValue()));
        List<ParameterDeclaration> parameters = operation.getParameters();

        assertParameterPlacement(findParameter(parameters, "victim"), KILL_WITH_GROUP, 1);
        assertParameterPlacement(findParameter(parameters, "goodbyeMessage"), KILL_WITH_GROUP, 2);
    }

    @Test
    public void heisenbergPointer() throws Exception
    {
        setDescriber(describerFor(HeisenbergPointer.class));
        describeTestModule();
    }

    @Test
    public void heisenbergPointerPlusExternalConfig() throws Exception
    {
        setDescriber(describerFor(HeisenbergPointerPlusExternalConfig.class));
        Declaration declaration = getDescriber().describe(new DefaultDescribingContext()).getRootDeclaration().getDeclaration();

        assertExtensionProperties(declaration);
        assertThat(declaration.getConfigurations().size(), equalTo(2));

        ConfigurationDeclaration configuration = declaration.getConfigurations().get(1);
        assertThat(configuration, is(notNullValue()));
        assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
        assertThat(configuration.getParameters(), hasSize(26));
        assertParameter(configuration.getParameters(), "extendedProperty", "", toMetadataType(String.class), true, SUPPORTED, null);
    }

    @Test(expected = IllegalConfigurationModelDefinitionException.class)
    public void heisenbergWithOperationsConfig() throws Exception
    {
        describerFor(HeisenbergWithSameOperationsAndConfigs.class).describe(new DefaultDescribingContext());
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void heisenbergWithParameterGroupAsOptional() throws Exception
    {
        describerFor(HeisenbergWithParameterGroupAsOptional.class).describe(new DefaultDescribingContext());
    }

    @Test(expected = IllegalModelDefinitionException.class)
    public void heisenbergWithMoreThanOneConfigInOperation() throws Exception
    {
        describerFor(HeisenbergWithInvalidOperation.class).describe(new DefaultDescribingContext());
    }

    @Test(expected = IllegalOperationModelDefinitionException.class)
    public void heisenbergWithOperationPointingToExtension() throws Exception
    {
        describerFor(HeisenbergWithOperationsPointingToExtension.class).describe(new DefaultDescribingContext());
    }

    @Test(expected = IllegalConfigurationModelDefinitionException.class)
    public void heisenbergWithOperationPointingToExtensionAndDefaultConfig() throws Exception
    {
        describerFor(HeisenbergWithOperationsPointingToExtensionAndDefaultConfig.class).describe(new DefaultDescribingContext());
    }

    @Test
    public void messageOperationWithoutGenerics() throws Exception
    {
        Descriptor descriptor = describerFor(HeisenbergWithGenericlessMessageOperation.class).describe(new DefaultDescribingContext());
        OperationDeclaration operation = getOperation(descriptor.getRootDeclaration().getDeclaration(), "noGenerics");

        assertThat(operation.getReturnType(), is(instanceOf(AnyType.class)));
        assertThat(operation.getAttributesType(), is(instanceOf(NullType.class)));
    }

    private void assertTestModuleConfiguration(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConfigurations(), hasSize(1));
        ConfigurationDeclaration conf = declaration.getConfigurations().get(0);
        assertThat(conf.getName(), equalTo(DEFAULT_CONFIG_NAME));

        List<ParameterDeclaration> parameters = conf.getParameters();
        assertThat(parameters, hasSize(25));

        assertParameter(parameters, "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
        assertParameter(parameters, "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);
        assertParameter(parameters, "enemies", "", listOfString(), true, SUPPORTED, null);
        assertParameter(parameters, "money", "", toMetadataType(BigDecimal.class), true, SUPPORTED, null);
        assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
        assertParameter(parameters, "cancer", "", toMetadataType(boolean.class), true, SUPPORTED, null);
        assertParameter(parameters, "dateOfBirth", "", toMetadataType(Date.class), true, SUPPORTED, null);
        assertParameter(parameters, "dateOfDeath", "", toMetadataType(Calendar.class), true, SUPPORTED, null);

        assertParameter(parameters, "recipe", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                                .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                                .ofValue(TYPE_BUILDER.numberType().id("java.lang.Long"))
                                .build(),
                        false, SUPPORTED, null);

        assertParameter(parameters, "ricinPacks", "", arrayOf(Set.class, objectTypeBuilder(Ricin.class)), false, SUPPORTED, null);

        assertParameter(parameters, "nextDoor", "", toMetadataType(KnockeableDoor.class), false, SUPPORTED, null);
        assertParameter(parameters, "candidateDoors", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                                .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                                .ofValue(objectTypeBuilder(KnockeableDoor.class))
                                .build(),
                        false, SUPPORTED, null);

        assertParameter(parameters, "initialHealth", "", toMetadataType(HealthStatus.class), false, SUPPORTED, "CANCER");
        assertParameter(parameters, "finalHealth", "", toMetadataType(HealthStatus.class), true, SUPPORTED, null);
        assertParameter(parameters, "labAddress", "", toMetadataType(String.class), false, REQUIRED, null);
        assertParameter(parameters, "firstEndevour", "", toMetadataType(String.class), false, NOT_SUPPORTED, null);
        assertParameter(parameters, "weapon", "", toMetadataType(Weapon.class), false, SUPPORTED, null);
        assertParameter(parameters, "moneyFunction", "", TYPE_BUILDER.objectType()
                                .id(Function.class.getName())
                                .with(new GenericTypesAnnotation(asList(MuleEvent.class.getName(), Integer.class.getName())))
                                .build(),
                        false, SUPPORTED, null);
        assertParameter(parameters, "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), false, SUPPORTED, null);
        assertParameter(parameters, "wildCardList", "", arrayOf(List.class, objectTypeBuilder(Object.class)), false, SUPPORTED, null);
        assertParameter(parameters, "wildCardWeaponMap", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                                .ofKey(objectTypeBuilder(Weapon.class))
                                .ofValue(objectTypeBuilder(Object.class))
                                .build(),
                        false, SUPPORTED, null);

        assertParameter(parameters, "monthlyIncomes", "", arrayOf(List.class, TYPE_BUILDER.numberType().id(Long.class.getName())), true, SUPPORTED, null);
        assertParameter(parameters, "labeledRicin", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                                .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                                .ofValue(objectTypeBuilder(Ricin.class))
                                .build(),
                        false, SUPPORTED, null);
        assertParameter(parameters, "deathsBySeasons", "", TYPE_BUILDER.dictionaryType().id(Map.class.getName())
                                .ofKey(TYPE_BUILDER.stringType().id(String.class.getName()))
                                .ofValue(TYPE_BUILDER.arrayType().id(List.class.getName()).of(TYPE_BUILDER.stringType().id(String.class.getName())))
                                .build(),
                        false, SUPPORTED, null);
    }

    private void assertExtensionProperties(Declaration declaration)
    {
        assertThat(declaration, is(notNullValue()));

        assertThat(declaration.getName(), is(HEISENBERG));
        assertThat(declaration.getDescription(), is(EXTENSION_DESCRIPTION));
        assertThat(declaration.getVersion(), is(EXTENSION_VERSION));
    }

    private void assertTestModuleOperations(Declaration declaration) throws Exception
    {
        assertThat(declaration.getOperations(), hasSize(22));
        assertOperation(declaration, SAY_MY_NAME_OPERATION, "");
        assertOperation(declaration, GET_ENEMY_OPERATION, "");
        assertOperation(declaration, KILL_OPERATION, "");
        assertOperation(declaration, KILL_CUSTOM_OPERATION, "");
        assertOperation(declaration, KILL_WITH_WEAPON, "");
        assertOperation(declaration, KILL_WITH_MULTIPLES_WEAPONS, "");
        assertOperation(declaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS, "");
        assertOperation(declaration, GET_PAYMENT_FROM_EVENT_OPERATION, "");
        assertOperation(declaration, GET_PAYMENT_FROM_MESSAGE_OPERATION, "");
        assertOperation(declaration, DIE, "");
        assertOperation(declaration, KILL_MANY, "");
        assertOperation(declaration, KILL_ONE, "");
        assertOperation(declaration, LAUNDER_MONEY, "");
        assertOperation(declaration, INJECTED_EXTENSION_MANAGER, "");
        assertOperation(declaration, ALIAS, "");
        assertOperation(declaration, CALL_SAUL, "");
        assertOperation(declaration, CALL_GUS_FRING, "");
        assertOperation(declaration, GET_SAUL_PHONE, "");

        OperationDeclaration operation = getOperation(declaration, SAY_MY_NAME_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, GET_ENEMY_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertThat(operation.getReturnType(), equalTo(toMetadataType(String.class)));
        assertThat(operation.getAttributesType(), equalTo(toMetadataType(Integer.class)));
        assertParameter(operation.getParameters(), "index", "", toMetadataType(int.class), false, SUPPORTED, "0");

        operation = getOperation(declaration, KILL_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_WITH_WEAPON);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(3)) ;
        assertParameter(operation.getParameters(), "weapon", "", toMetadataType(Weapon.class), true, SUPPORTED, null);
        assertParameter(operation.getParameters(), "type", "", toMetadataType(Weapon.WeaponType.class), true, SUPPORTED, null);
        assertParameter(operation.getParameters(), "weaponAttributes", "", toMetadataType(Weapon.WeaponAttributes.class), true, SUPPORTED, null);


        operation = getOperation(declaration, KILL_WITH_MULTIPLES_WEAPONS);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "weaponList", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "wildCardWeapons", "", arrayOf(List.class, objectTypeBuilder(Weapon.class)), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_CUSTOM_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", toMetadataType(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", toMetadataType(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, GET_PAYMENT_FROM_EVENT_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, GET_PAYMENT_FROM_MESSAGE_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, LAUNDER_MONEY);
        assertParameter(operation.getParameters(), "amount", "", toMetadataType(long.class), true, SUPPORTED, null);

        operation = getOperation(declaration, INJECTED_EXTENSION_MANAGER);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, ALIAS);
        assertParameter(operation.getParameters(), "greeting", "", toMetadataType(String.class), true, SUPPORTED, null);
        assertParameter(operation.getParameters(), "myName", "", toMetadataType(String.class), false, SUPPORTED, HEISENBERG);
        assertParameter(operation.getParameters(), "age", "", toMetadataType(Integer.class), false, SUPPORTED, AGE);

        operation = getOperation(declaration, KNOCK);
        assertParameter(operation.getParameters(), "door", "", toMetadataType(KnockeableDoor.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KNOCK_MANY);
        assertParameter(operation.getParameters(), "doors", "", arrayOf(List.class, objectTypeBuilder(KnockeableDoor.class)), true, SUPPORTED, null);

        operation = getOperation(declaration, CALL_SAUL);
        assertThat(operation.getParameters(), is(empty()));
        ConnectionTypeModelProperty connectionType = operation.getModelProperty(ConnectionTypeModelProperty.class).get();
        assertThat(connectionType.getConnectionType(), equalTo(HeisenbergConnection.class));

        operation = getOperation(declaration, CURE_CANCER);
        assertThat(operation.getParameters(), is(empty()));
        java.util.Optional<ExceptionEnricherFactory> exceptionEnricherFactory = operation.getExceptionEnricherFactory();
        assertThat(exceptionEnricherFactory.isPresent(), is(true));
        assertThat(exceptionEnricherFactory.get().createEnricher(), instanceOf(CureCancerExceptionEnricher.class));

        operation = getOperation(declaration, CALL_GUS_FRING);
        assertThat(operation.getParameters(), is(empty()));
        java.util.Optional<ExceptionEnricherFactory> exceptionEnricherFactory2 = operation.getExceptionEnricherFactory();
        assertThat(exceptionEnricherFactory2.isPresent(), is(false));

        operation = getOperation(declaration, IGNORED_OPERATION);
        assertThat(operation, is(nullValue()));
    }

    private void assertTestModuleConnectionProviders(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConnectionProviders(), hasSize(1));
        ConnectionProviderDeclaration connectionProvider = declaration.getConnectionProviders().get(0);
        assertThat(connectionProvider.getName(), is(DEFAULT_CONNECTION_PROVIDER_NAME));

        List<ParameterDeclaration> parameters = connectionProvider.getParameters();
        assertThat(parameters, hasSize(2));

        assertParameter(parameters, "saulPhoneNumber", "", toMetadataType(String.class), false, SUPPORTED, SAUL_OFFICE_NUMBER);
        assertParameter(parameters, TLS_ATTRIBUTE_NAME, "", toMetadataType(TlsContextFactory.class), false, NOT_SUPPORTED, null);
        ImplementingTypeModelProperty typeModelProperty = connectionProvider.getModelProperty(ImplementingTypeModelProperty.class).get();
        assertThat(typeModelProperty.getType(), equalTo(HeisenbergConnectionProvider.class));
    }

    private void assertTestModuleMessageSource(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConnectionProviders(), hasSize(1));
        SourceDeclaration source = declaration.getMessageSources().get(0);
        assertThat(source.getName(), is(SOURCE_NAME));

        List<ParameterDeclaration> parameters = source.getParameters();
        assertThat(parameters, hasSize(2));

        assertParameter(parameters, SOURCE_PARAMETER, "", toMetadataType(int.class), true, SUPPORTED, null);
        ImplementingTypeModelProperty typeModelProperty = source.getModelProperty(ImplementingTypeModelProperty.class).get();
        assertThat(typeModelProperty.getType(), equalTo(HeisenbergSource.class));
    }

    private void assertOperation(Declaration declaration,
                                 String operationName,
                                 String operationDescription) throws Exception
    {
        OperationDeclaration operation = getOperation(declaration, operationName);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getDescription(), equalTo(operationDescription));
    }

    private void assertParameter(List<ParameterDeclaration> parameters,
                                 String name,
                                 String description,
                                 MetadataType metadataType,
                                 boolean required,
                                 ExpressionSupport expressionSupport,
                                 Object defaultValue)
    {
        ParameterDeclaration param = findParameter(parameters, name);
        assertThat(param, is(notNullValue()));

        assertThat(param.getName(), equalTo(name));
        assertThat(param.getDescription(), equalTo(description));
        assertThat(param.getType(), equalTo(metadataType));
        assertThat(param.isRequired(), is(required));
        assertThat(param.getExpressionSupport(), is(expressionSupport));
        assertThat(param.getDefaultValue(), equalTo(defaultValue));
    }

    private void assertParameterPlacement(ParameterDeclaration param, String groupName, Integer order)
    {
        DisplayModelProperty display = param.getModelProperty(DisplayModelProperty.class).get();

        if (groupName != null)
        {
            assertThat(display.getGroupName(), is(groupName));
        }
        if (order != null)
        {
            assertThat(display.getOrder(), is(order));
        }
    }

    private void assertParameterDisplayName(ParameterDeclaration param, String displayName)
    {
        DisplayModelProperty display = param.getModelProperty(DisplayModelProperty.class).get();
        assertThat(display.getDisplayName(), is(displayName));
    }

    private ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name)
    {
        return (ParameterDeclaration) CollectionUtils.find(parameters, object -> name.equals(((ParameterDeclaration) object).getName()));
    }

    private MetadataType listOfString()
    {
        return TYPE_BUILDER.arrayType().id(List.class.getName())
                .of(TYPE_BUILDER.stringType().id(String.class.getName()))
                .build();
    }

    protected void assertModelProperties(Declaration declaration)
    {
        ImplementingTypeModelProperty implementingTypeModelProperty = declaration.getModelProperty(ImplementingTypeModelProperty.class).get();
        assertThat(implementingTypeModelProperty, is(notNullValue()));
        assertThat(HeisenbergExtension.class.isAssignableFrom(implementingTypeModelProperty.getType()), is(true));
    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Xml(schemaVersion = SCHEMA_VERSION)
    @Configurations(HeisenbergExtension.class)
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    @Providers(HeisenbergConnectionProvider.class)
    @Sources(HeisenbergSource.class)
    public static class HeisenbergPointer extends HeisenbergExtension
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Xml(schemaVersion = SCHEMA_VERSION)
    @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    public static class HeisenbergPointerPlusExternalConfig
    {

    }

    @Configuration(name = EXTENDED_CONFIG_NAME, description = EXTENDED_CONFIG_DESCRIPTION)
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Operations({DuplicateConfigOperation.class})
    public static class HeisenbergWithInvalidOperation extends HeisenbergExtension
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    public static class HeisenbergWithParameterGroupAsOptional extends HeisenbergExtension
    {

        @ParameterGroup
        @Optional
        private ExtendedPersonalInfo personalInfo;

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Operations(HeisenbergAlternateConfig.class)
    @Configurations(HeisenbergAlternateConfig.class)
    public static class HeisenbergWithSameOperationsAndConfigs extends HeisenbergExtension
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Operations(HeisenbergExtension.class)
    @Configurations(HeisenbergIsolatedConfig.class)
    public static class HeisenbergWithOperationsPointingToExtension extends HeisenbergExtension
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Operations(HeisenbergExtension.class)
    public static class HeisenbergWithOperationsPointingToExtensionAndDefaultConfig extends HeisenbergExtension
    {

    }

    @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
    @Operations({HeisenbergExtension.class, GenericlessMessageOperation.class})
    public static class HeisenbergWithGenericlessMessageOperation
    {

    }

    public static class DuplicateConfigOperation
    {

        public Long launder(@UseConfig HeisenbergExtension config, @UseConfig HeisenbergExtension config2)
        {
            return 10L;
        }

    }

    public static class GenericlessMessageOperation
    {

        public MuleMessage noGenerics()
        {
            return null;
        }
    }

    public static class HeisenbergAlternateConfig extends HeisenbergExtension
    {

        @Parameter
        private String extendedProperty;

        public String getExtendedProperty()
        {
            return extendedProperty;
        }

        public void setExtendedProperty(String extendedProperty)
        {
            this.extendedProperty = extendedProperty;
        }

    }

    public static class HeisenbergIsolatedConfig
    {

        @Parameter
        private String extendedProperty;

        public String getExtendedProperty()
        {
            return extendedProperty;
        }

        public void setExtendedProperty(String extendedProperty)
        {
            this.extendedProperty = extendedProperty;
        }
    }

}

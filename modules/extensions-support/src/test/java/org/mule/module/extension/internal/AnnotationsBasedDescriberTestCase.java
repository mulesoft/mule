/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.annotation.api.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.extension.api.introspection.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.extension.api.introspection.ExpressionSupport.REQUIRED;
import static org.mule.extension.api.introspection.ExpressionSupport.SUPPORTED;
import static org.mule.module.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.module.extension.HeisenbergExtension.AGE;
import static org.mule.module.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.module.extension.HeisenbergExtension.SCHEMA_VERSION;
import static org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import org.mule.api.MuleEvent;
import org.mule.config.MuleManifest;
import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.Sources;
import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.exception.IllegalModelDefinitionException;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExceptionEnricherFactory;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.module.extension.HeisenbergConnection;
import org.mule.module.extension.HeisenbergConnectionProvider;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.HeisenbergSource;
import org.mule.module.extension.MoneyLaunderingOperation;
import org.mule.module.extension.exception.CureCancerExceptionEnricher;
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
    private static final String EXTENSION_VERSION = MuleManifest.getProductVersion();

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
        assertParameter(configuration.getParameters(), "extendedProperty", "", DataType.of(String.class), true, SUPPORTED, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void heisenbergWithOperationsConfig() throws Exception
    {
        describerFor(HeisenbergWithOperations.class).describe(new DefaultDescribingContext());
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

    private void assertTestModuleConfiguration(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConfigurations(), hasSize(1));
        ConfigurationDeclaration conf = declaration.getConfigurations().get(0);
        assertThat(conf.getName(), equalTo(DEFAULT_CONFIG_NAME));

        List<ParameterDeclaration> parameters = conf.getParameters();
        assertThat(parameters, hasSize(25));

        assertParameter(parameters, "myName", "", DataType.of(String.class), false, SUPPORTED, HEISENBERG);
        assertParameter(parameters, "age", "", DataType.of(Integer.class), false, SUPPORTED, AGE);
        assertParameter(parameters, "enemies", "", DataType.of(List.class, String.class), true, SUPPORTED, null);
        assertParameter(parameters, "money", "", DataType.of(BigDecimal.class), true, SUPPORTED, null);
        assertParameter(parameters, "cancer", "", DataType.of(boolean.class), true, SUPPORTED, null);
        assertParameter(parameters, "cancer", "", DataType.of(boolean.class), true, SUPPORTED, null);
        assertParameter(parameters, "dateOfBirth", "", DataType.of(Date.class), true, SUPPORTED, null);
        assertParameter(parameters, "dateOfDeath", "", DataType.of(Calendar.class), true, SUPPORTED, null);
        assertParameter(parameters, "recipe", "", DataType.of(Map.class, String.class, Long.class), false, SUPPORTED, null);
        assertParameter(parameters, "ricinPacks", "", DataType.of(Set.class, Ricin.class), false, SUPPORTED, null);
        assertParameter(parameters, "nextDoor", "", DataType.of(KnockeableDoor.class), false, SUPPORTED, null);
        assertParameter(parameters, "candidateDoors", "", DataType.of(Map.class, String.class, KnockeableDoor.class), false, SUPPORTED, null);
        assertParameter(parameters, "initialHealth", "", DataType.of(HealthStatus.class), false, SUPPORTED, "CANCER");
        assertParameter(parameters, "finalHealth", "", DataType.of(HealthStatus.class), true, SUPPORTED, null);
        assertParameter(parameters, "labAddress", "", DataType.of(String.class), false, REQUIRED, null);
        assertParameter(parameters, "firstEndevour", "", DataType.of(String.class), false, NOT_SUPPORTED, null);
        assertParameter(parameters, "weapon", "", DataType.of(Weapon.class), false, SUPPORTED, null);
        assertParameter(parameters, "moneyFunction", "", DataType.of(Function.class, MuleEvent.class, Integer.class), false, SUPPORTED, null);
        assertParameter(parameters, "wildCardWeapons", "", DataType.of(List.class, Weapon.class), false, SUPPORTED, null);
        assertParameter(parameters, "wildCardList", "", DataType.of(List.class, Object.class), false, SUPPORTED, null);
        assertParameter(parameters, "wildCardWeaponMap", "", DataType.of(Map.class, Weapon.class, Object.class), false, SUPPORTED, null);
        assertParameter(parameters, "monthlyIncomes", "", DataType.of(List.class, Long.class), true, SUPPORTED, null);
        assertParameter(parameters, "labeledRicin", "", DataType.of(Map.class, String.class, Ricin.class), false, SUPPORTED, null);
        assertParameter(parameters, "deathsBySeasons", "", DataType.of(Map.class, DataType.of(String.class), DataType.of(List.class, DataType.of(String.class))), false, SUPPORTED, null);
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
        assertParameter(operation.getParameters(), "index", "", DataType.of(int.class), false, SUPPORTED, "0");

        operation = getOperation(declaration, KILL_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_WITH_WEAPON);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "weapon", "", DataType.of(Weapon.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_WITH_MULTIPLES_WEAPONS);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "weaponList", "", DataType.of(List.class, Weapon.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_WITH_MULTIPLE_WILDCARD_WEAPONS);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "wildCardWeapons", "", DataType.of(List.class, Weapon.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_CUSTOM_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, GET_PAYMENT_FROM_EVENT_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, GET_PAYMENT_FROM_MESSAGE_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, LAUNDER_MONEY);
        assertParameter(operation.getParameters(), "amount", "", DataType.of(long.class), true, SUPPORTED, null);

        operation = getOperation(declaration, INJECTED_EXTENSION_MANAGER);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, ALIAS);
        assertParameter(operation.getParameters(), "greeting", "", DataType.of(String.class), true, SUPPORTED, null);
        assertParameter(operation.getParameters(), "myName", "", DataType.of(String.class), false, SUPPORTED, HEISENBERG);
        assertParameter(operation.getParameters(), "age", "", DataType.of(Integer.class), false, SUPPORTED, AGE);

        operation = getOperation(declaration, KNOCK);
        assertParameter(operation.getParameters(), "door", "", DataType.of(KnockeableDoor.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KNOCK_MANY);
        assertParameter(operation.getParameters(), "doors", "", DataType.of(List.class, KnockeableDoor.class), true, SUPPORTED, null);

        operation = getOperation(declaration, CALL_SAUL);
        assertThat(operation.getParameters(), is(empty()));
        ConnectionTypeModelProperty connectionType = operation.getModelProperty(ConnectionTypeModelProperty.KEY);
        assertThat(connectionType, is(notNullValue()));
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
    }

    private void assertTestModuleConnectionProviders(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConnectionProviders(), hasSize(1));
        ConnectionProviderDeclaration connectionProvider = declaration.getConnectionProviders().get(0);
        assertThat(connectionProvider.getName(), is(DEFAULT_CONNECTION_PROVIDER_NAME));

        List<ParameterDeclaration> parameters = connectionProvider.getParameters();
        assertThat(parameters, hasSize(1));

        assertParameter(parameters, "saulPhoneNumber", "", DataType.of(String.class), false, SUPPORTED, SAUL_OFFICE_NUMBER);
        ImplementingTypeModelProperty typeModelProperty = connectionProvider.getModelProperty(ImplementingTypeModelProperty.KEY);
        assertThat(typeModelProperty, is(notNullValue()));
        assertThat(typeModelProperty.getType(), equalTo(HeisenbergConnectionProvider.class));
    }

    private void assertTestModuleMessageSource(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConnectionProviders(), hasSize(1));
        SourceDeclaration source = declaration.getMessageSources().get(0);
        assertThat(source.getName(), is(SOURCE_NAME));

        List<ParameterDeclaration> parameters = source.getParameters();
        assertThat(parameters, hasSize(1));

        assertParameter(parameters, SOURCE_PARAMETER, "", DataType.of(int.class), true, SUPPORTED, null);
        ImplementingTypeModelProperty typeModelProperty = source.getModelProperty(ImplementingTypeModelProperty.KEY);
        assertThat(typeModelProperty, is(notNullValue()));
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
                                 DataType dataType,
                                 boolean required,
                                 ExpressionSupport expressionSupport,
                                 Object defaultValue)
    {
        ParameterDeclaration param = findParameter(parameters, name);
        assertThat(param, is(notNullValue()));

        assertThat(param.getName(), equalTo(name));
        assertThat(param.getDescription(), equalTo(description));
        assertThat(param.getType(), equalTo(dataType));
        assertThat(param.isRequired(), is(required));
        assertThat(param.getExpressionSupport(), is(expressionSupport));
        assertThat(param.getDefaultValue(), equalTo(defaultValue));
    }

    private ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name)
    {
        return (ParameterDeclaration) CollectionUtils.find(parameters, object -> name.equals(((ParameterDeclaration) object).getName()));
    }

    protected void assertModelProperties(Declaration declaration)
    {
        ImplementingTypeModelProperty implementingTypeModelProperty = declaration.getModelProperty(ImplementingTypeModelProperty.KEY);
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
    public static class HeisenbergWithOperations extends HeisenbergExtension
    {

        @Operation
        public void invalid()
        {
        }
    }

    public static class DuplicateConfigOperation
    {

        @Operation
        public Long launder(@UseConfig HeisenbergExtension config, @UseConfig HeisenbergExtension config2)
        {
            return 10L;
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

}

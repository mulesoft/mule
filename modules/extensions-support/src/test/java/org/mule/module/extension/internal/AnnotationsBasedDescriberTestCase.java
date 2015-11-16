/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
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
import static org.mule.module.extension.HeisenbergExtension.EXTENSION_NAME;
import static org.mule.module.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.module.extension.HeisenbergExtension.NAMESPACE;
import static org.mule.module.extension.HeisenbergExtension.SCHEMA_LOCATION;
import static org.mule.module.extension.HeisenbergExtension.SCHEMA_VERSION;
import static org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber.DEFAULT_CONNECTION_PROVIDER_NAME;
import org.mule.config.MuleManifest;
import org.mule.extension.annotation.api.Configuration;
import org.mule.extension.annotation.api.Configurations;
import org.mule.extension.annotation.api.Extension;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.Operations;
import org.mule.extension.annotation.api.Parameter;
import org.mule.extension.annotation.api.capability.Xml;
import org.mule.extension.annotation.api.connector.Providers;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.Declaration;
import org.mule.extension.api.introspection.declaration.fluent.Descriptor;
import org.mule.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.module.extension.HealthStatus;
import org.mule.module.extension.HeisenbergConnection;
import org.mule.module.extension.HeisenbergConnectionProvider;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.KnockeableDoor;
import org.mule.module.extension.MoneyLaunderingOperation;
import org.mule.module.extension.Ricin;
import org.mule.module.extension.internal.model.property.ConnectionTypeModelProperty;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.size.SmallTest;
import org.mule.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String SAY_MY_NAME_OPERATION = "sayMyName";
    private static final String GET_ENEMY_OPERATION = "getEnemy";
    private static final String KILL_OPERATION = "kill";
    private static final String KILL_CUSTOM_OPERATION = "killWithCustomMessage";
    private static final String HIDE_METH_IN_EVENT_OPERATION = "hideMethInEvent";
    private static final String HIDE_METH_IN_MESSAGE_OPERATION = "hideMethInMessage";
    private static final String DIE = "die";
    private static final String KILL_MANY = "killMany";
    private static final String KILL_ONE = "killOne";
    private static final String LAUNDER_MONEY = "launder";
    private static final String INJECTED_EXTENSION_MANAGER = "getInjectedExtensionManager";
    private static final String ALIAS = "alias";
    private static final String KNOCK = "knock";
    private static final String KNOCK_MANY = "knockMany";
    private static final String CALL_SAUL = "callSaul";
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
        setDescriber(describerFor(HeisengergPointerPlusExternalConfig.class));
        Declaration declaration = getDescriber().describe(new DefaultDescribingContext()).getRootDeclaration().getDeclaration();

        assertExtensionProperties(declaration);
        assertThat(declaration.getConfigurations().size(), equalTo(2));

        ConfigurationDeclaration configuration = declaration.getConfigurations().get(1);
        assertThat(configuration, is(notNullValue()));
        assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
        assertThat(configuration.getParameters(), hasSize(1));
        assertParameter(configuration.getParameters(), "extendedProperty", "", DataType.of(String.class), true, SUPPORTED, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void heisenbergWithOperationsConfig() throws Exception
    {
        describerFor(HeisenbergWithOperations.class).describe(new DefaultDescribingContext());
    }

    private void assertTestModuleConfiguration(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConfigurations(), hasSize(1));
        ConfigurationDeclaration conf = declaration.getConfigurations().get(0);
        assertThat(conf.getName(), equalTo(DEFAULT_CONFIG_NAME));

        List<ParameterDeclaration> parameters = conf.getParameters();
        assertThat(parameters, hasSize(15));

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
        assertParameter(parameters, "initialHealth", "", DataType.of(HealthStatus.class), true, SUPPORTED, null);
        assertParameter(parameters, "finalHealth", "", DataType.of(HealthStatus.class), true, SUPPORTED, null);
        assertParameter(parameters, "labAddress", "", DataType.of(String.class), false, REQUIRED, null);
        assertParameter(parameters, "firstEndevour", "", DataType.of(String.class), false, NOT_SUPPORTED, null);
    }

    private void assertExtensionProperties(Declaration declaration)
    {
        assertThat(declaration, is(notNullValue()));

        assertThat(declaration.getName(), is(EXTENSION_NAME));
        assertThat(declaration.getDescription(), is(EXTENSION_DESCRIPTION));
        assertThat(declaration.getVersion(), is(EXTENSION_VERSION));
    }

    private void assertTestModuleOperations(Declaration declaration) throws Exception
    {
        assertThat(declaration.getOperations(), hasSize(15));
        assertOperation(declaration, SAY_MY_NAME_OPERATION, "");
        assertOperation(declaration, GET_ENEMY_OPERATION, "");
        assertOperation(declaration, KILL_OPERATION, "");
        assertOperation(declaration, KILL_CUSTOM_OPERATION, "");
        assertOperation(declaration, HIDE_METH_IN_EVENT_OPERATION, "");
        assertOperation(declaration, HIDE_METH_IN_MESSAGE_OPERATION, "");
        assertOperation(declaration, DIE, "");
        assertOperation(declaration, KILL_MANY, "");
        assertOperation(declaration, KILL_ONE, "");
        assertOperation(declaration, LAUNDER_MONEY, "");
        assertOperation(declaration, INJECTED_EXTENSION_MANAGER, "");
        assertOperation(declaration, ALIAS, "");
        assertOperation(declaration, CALL_SAUL, "");

        OperationDeclaration operation = getOperation(declaration, SAY_MY_NAME_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, GET_ENEMY_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "index", "", DataType.of(int.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, KILL_CUSTOM_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, SUPPORTED, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, SUPPORTED, null);

        operation = getOperation(declaration, HIDE_METH_IN_EVENT_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, HIDE_METH_IN_MESSAGE_OPERATION);
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

    @Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION)
    @Xml(schemaLocation = SCHEMA_LOCATION, namespace = NAMESPACE, schemaVersion = SCHEMA_VERSION)
    @Configurations(HeisenbergExtension.class)
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    @Providers(HeisenbergConnectionProvider.class)
    public static class HeisenbergPointer extends HeisenbergExtension
    {

    }

    @Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION)
    @Xml(schemaLocation = SCHEMA_LOCATION, namespace = NAMESPACE, schemaVersion = SCHEMA_VERSION)
    @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    public static class HeisengergPointerPlusExternalConfig
    {

    }

    @Configuration(name = EXTENDED_CONFIG_NAME, description = EXTENDED_CONFIG_DESCRIPTION)
    @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class})
    public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig
    {

    }

    @Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION)
    public static class HeisenbergWithOperations extends HeisenbergExtension
    {

        @Operation
        public void invalid()
        {
        }
    }

    public static class HeisenbergAlternateConfig
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

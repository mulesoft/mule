/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extensions.annotations.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.module.extensions.HeisenbergExtension.AGE;
import static org.mule.module.extensions.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.module.extensions.HeisenbergExtension.EXTENSION_NAME;
import static org.mule.module.extensions.HeisenbergExtension.EXTENSION_VERSION;
import static org.mule.module.extensions.HeisenbergExtension.HEISENBERG;
import static org.mule.module.extensions.HeisenbergExtension.NAMESPACE;
import static org.mule.module.extensions.HeisenbergExtension.SCHEMA_LOCATION;
import static org.mule.module.extensions.HeisenbergExtension.SCHEMA_VERSION;
import org.mule.extensions.annotations.Configurations;
import org.mule.extensions.annotations.Operations;
import org.mule.extensions.annotations.Parameter;
import org.mule.extensions.annotations.capability.Xml;
import org.mule.extensions.introspection.DataType;
import org.mule.extensions.introspection.Describer;
import org.mule.extensions.introspection.declaration.ConfigurationDeclaration;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.extensions.introspection.declaration.Declaration;
import org.mule.extensions.introspection.declaration.OperationDeclaration;
import org.mule.extensions.introspection.declaration.ParameterDeclaration;
import org.mule.module.extensions.Door;
import org.mule.module.extensions.HealthStatus;
import org.mule.module.extensions.HeisenbergAliasOperations;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.HeisenbergOperations;
import org.mule.module.extensions.Ricin;
import org.mule.module.extensions.internal.capability.metadata.ImplementedTypeCapability;
import org.mule.module.extensions.internal.introspection.AnnotationsBasedDescriber;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Predicate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class AnnotationsBasedDescriberTestCase extends AbstractMuleTestCase
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
    private static final String ALIAS = "alias";

    private Describer describer;

    @Before
    public void setUp()
    {
        describer = describerFor(HeisenbergExtension.class);
    }

    protected Describer describerFor(final Class<?> type)
    {
        return new AnnotationsBasedDescriber(type);
    }

    @Test
    public void describeTestModule() throws Exception
    {
        Construct construct = describer.describe();

        Declaration declaration = construct.getRootConstruct().getDeclaration();
        assertExtensionProperties(declaration);

        assertTestModuleConfiguration(declaration);
        assertTestModuleOperations(declaration);

        assertCapabilities(declaration);
    }

    @Test
    public void heisengergPointer() throws Exception
    {
        describer = describerFor(HeisenbergPointer.class);
        describeTestModule();
    }

    @Test
    public void heisengergPointerPlusExternalConfig() throws Exception
    {
        describer = describerFor(HeisengergPointerPlusExternalConfig.class);
        Declaration declaration = describer.describe().getRootConstruct().getDeclaration();

        assertExtensionProperties(declaration);
        assertThat(declaration.getConfigurations().size(), equalTo(2));

        ConfigurationDeclaration configuration = declaration.getConfigurations().get(1);
        assertThat(configuration, is(notNullValue()));
        assertThat(configuration.getName(), equalTo(EXTENDED_CONFIG_NAME));
        assertThat(configuration.getParameters(), hasSize(1));
        assertParameter(configuration.getParameters(), "extendedProperty", "", DataType.of(String.class), true, true, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void heisenbergWithOperationsConfig() throws Exception
    {
        describerFor(HeisenbergWithOperations.class).describe();
    }

    @Test
    public void aliasImplementsOperations() throws Exception
    {
        Declaration declaration = describer.describe().getRootConstruct().getDeclaration();
        OperationDeclaration operation = getOperation(declaration, "alias");
        assertThat(operation.getCapabilities(), is(not(emptyIterable())));

        List<ImplementedTypeCapability> capabilities = new ArrayList<>();

        for (Object object : operation.getCapabilities())
        {
            if (object instanceof ImplementedTypeCapability)
            {
                capabilities.add((ImplementedTypeCapability) object);
            }
        }

        assertThat(capabilities, hasSize(1));
        ImplementedTypeCapability<HeisenbergExtension> capability = capabilities.get(0);
        assertThat(capability.getType(), is(sameInstance(HeisenbergExtension.class)));
    }

    private void assertTestModuleConfiguration(Declaration declaration) throws Exception
    {
        assertThat(declaration.getConfigurations(), hasSize(1));
        ConfigurationDeclaration conf = declaration.getConfigurations().get(0);
        assertThat(conf.getName(), equalTo(DEFAULT_CONFIG_NAME));

        List<ParameterDeclaration> parameters = conf.getParameters();
        assertThat(parameters, hasSize(13));

        assertParameter(parameters, "myName", "", DataType.of(String.class), false, true, HEISENBERG);
        assertParameter(parameters, "age", "", DataType.of(Integer.class), false, true, AGE);
        assertParameter(parameters, "enemies", "", DataType.of(List.class, String.class), true, true, null);
        assertParameter(parameters, "money", "", DataType.of(BigDecimal.class), true, true, null);
        assertParameter(parameters, "cancer", "", DataType.of(boolean.class), true, true, null);
        assertParameter(parameters, "cancer", "", DataType.of(boolean.class), true, true, null);
        assertParameter(parameters, "dateOfBirth", "", DataType.of(Date.class), true, true, null);
        assertParameter(parameters, "dateOfDeath", "", DataType.of(Calendar.class), true, true, null);
        assertParameter(parameters, "recipe", "", DataType.of(Map.class, String.class, Long.class), false, true, null);
        assertParameter(parameters, "ricinPacks", "", DataType.of(Set.class, Ricin.class), false, true, null);
        assertParameter(parameters, "nextDoor", "", DataType.of(Door.class), false, true, null);
        assertParameter(parameters, "candidateDoors", "", DataType.of(Map.class, String.class, Door.class), false, true, null);
        assertParameter(parameters, "initialHealth", "", DataType.of(HealthStatus.class), true, true, null);
        assertParameter(parameters, "finalHealth", "", DataType.of(HealthStatus.class), true, true, null);

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
        assertThat(declaration.getOperations(), hasSize(10));
        assertOperation(declaration, SAY_MY_NAME_OPERATION, "");
        assertOperation(declaration, GET_ENEMY_OPERATION, "");
        assertOperation(declaration, KILL_OPERATION, "");
        assertOperation(declaration, KILL_CUSTOM_OPERATION, "");
        assertOperation(declaration, HIDE_METH_IN_EVENT_OPERATION, "");
        assertOperation(declaration, HIDE_METH_IN_MESSAGE_OPERATION, "");
        assertOperation(declaration, DIE, "");
        assertOperation(declaration, KILL_MANY, "");
        assertOperation(declaration, KILL_ONE, "");
        assertOperation(declaration, ALIAS, "");

        OperationDeclaration operation = getOperation(declaration, SAY_MY_NAME_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, GET_ENEMY_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(1));
        assertParameter(operation.getParameters(), "index", "", DataType.of(int.class), true, true, null);

        operation = getOperation(declaration, KILL_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, true, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, true, null);

        operation = getOperation(declaration, KILL_CUSTOM_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters(), hasSize(2));
        assertParameter(operation.getParameters(), "victim", "", DataType.of(String.class), false, true, "#[payload]");
        assertParameter(operation.getParameters(), "goodbyeMessage", "", DataType.of(String.class), true, true, null);

        operation = getOperation(declaration, HIDE_METH_IN_EVENT_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, HIDE_METH_IN_MESSAGE_OPERATION);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getParameters().isEmpty(), is(true));

        operation = getOperation(declaration, ALIAS);
        assertParameter(operation.getParameters(), "myName", "", DataType.of(String.class), false, true, HEISENBERG);
        assertParameter(operation.getParameters(), "age", "", DataType.of(Integer.class), false, true, AGE);
    }

    private void assertOperation(Declaration declaration,
                                 String operationName,
                                 String operationDescription) throws Exception
    {

        OperationDeclaration operation = getOperation(declaration, operationName);
        assertThat(operation, is(notNullValue()));
        assertThat(operation.getDescription(), equalTo(operationDescription));
    }

    private OperationDeclaration getOperation(Declaration declaration, final String operationName)
    {
        return (OperationDeclaration) CollectionUtils.find(declaration.getOperations(), new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return ((OperationDeclaration) object).getName().equals(operationName);
            }
        });
    }

    private void assertParameter(List<ParameterDeclaration> parameters,
                                 String name,
                                 String description,
                                 DataType dataType,
                                 boolean required,
                                 boolean dynamic,
                                 Object defaultValue)
    {
        ParameterDeclaration param = findParameter(parameters, name);
        assertThat(param, is(notNullValue()));

        assertThat(param.getName(), equalTo(name));
        assertThat(param.getDescription(), equalTo(description));
        assertThat(param.getType(), equalTo(dataType));
        assertThat(param.isRequired(), is(required));
        assertThat(param.isDynamic(), is(dynamic));
        assertThat(param.getDefaultValue(), equalTo(defaultValue));
    }

    private ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name)
    {
        return (ParameterDeclaration) CollectionUtils.find(parameters, new Predicate()
        {
            @Override
            public boolean evaluate(Object object)
            {
                return name.equals(((ParameterDeclaration) object).getName());
            }
        });
    }

    protected void assertCapabilities(Declaration declaration)
    {
        // template method for asserting custom capabilities in modules that define them
    }

    @org.mule.extensions.annotations.Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION, version = EXTENSION_VERSION)
    @Xml(schemaLocation = SCHEMA_LOCATION, namespace = NAMESPACE, schemaVersion = SCHEMA_VERSION)
    @Configurations(HeisenbergExtension.class)
    @Operations({HeisenbergOperations.class, HeisenbergAliasOperations.class})
    public static class HeisenbergPointer extends HeisenbergExtension
    {

    }

    @org.mule.extensions.annotations.Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION, version = EXTENSION_VERSION)
    @Xml(schemaLocation = SCHEMA_LOCATION, namespace = NAMESPACE, schemaVersion = SCHEMA_VERSION)
    @Configurations({HeisenbergExtension.class, NamedHeisenbergAlternateConfig.class})
    @Operations({HeisenbergOperations.class, HeisenbergAliasOperations.class})
    public static class HeisengergPointerPlusExternalConfig
    {

    }

    @org.mule.extensions.annotations.Configuration(name = EXTENDED_CONFIG_NAME, description = EXTENDED_CONFIG_DESCRIPTION)
    @Operations({HeisenbergOperations.class, HeisenbergAliasOperations.class})
    public static class NamedHeisenbergAlternateConfig extends HeisenbergAlternateConfig
    {

    }

    @org.mule.extensions.annotations.Extension(name = EXTENSION_NAME, description = EXTENSION_DESCRIPTION, version = EXTENSION_VERSION)
    public static class HeisenbergWithOperations extends HeisenbergExtension
    {

        @org.mule.extensions.annotations.Operation
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

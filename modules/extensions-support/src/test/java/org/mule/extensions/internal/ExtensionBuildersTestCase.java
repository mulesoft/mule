/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.internal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import org.mule.extensions.introspection.api.ExtensionOperation;
import org.mule.extensions.introspection.api.ExtensionScope;
import org.mule.extensions.introspection.api.NoSuchConfigurationException;
import org.mule.extensions.introspection.api.NoSuchOperationException;
import org.mule.extensions.introspection.api.Capability;
import org.mule.extensions.introspection.api.Extension;
import org.mule.extensions.introspection.api.ExtensionConfiguration;
import org.mule.extensions.introspection.api.MuleExtensionOperationGroup;
import org.mule.extensions.introspection.api.MuleExtensionParameter;
import org.mule.extensions.introspection.api.MuleExtensionType;
import org.mule.extensions.introspection.spi.MuleExtensionBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.google.common.base.Optional;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExtensionBuildersTestCase extends AbstractMuleTestCase
{

    private static final String WS_CONSUMER = "WSConsumer";
    private static final String WS_CONSUMER_DESCRIPTION = "Generic Consumer for SOAP Web Services";
    private static final String VERSION = "3.6.0";
    private static final MuleExtensionType EXTENSION_TYPE = MuleExtensionType.MODULE;
    private static final String MIN_MULE_VERSION = VERSION;
    private static final String WSDL_LOCATION = "wsdlLocation";
    private static final String URI_TO_FIND_THE_WSDL = "URI to find the WSDL";
    private static final String SERVICE = "service";
    private static final String SERVICE_NAME = "Service Name";
    private static final String PORT = "port";
    private static final String SERVICE_PORT = "Service Port";
    private static final String ADDRESS = "address";
    private static final String SERVICE_ADDRESS = "Service address";
    private static final String CONSUMER = "consumer";
    private static final String GO_GET_THEM_TIGER = "Go get them tiger";
    private static final String OPERATION = "operation";
    private static final String THE_OPERATION_TO_USE = "The operation to use";
    private static final String MTOM_ENABLED = "mtomEnabled";
    private static final String MTOM_DESCRIPTION = "Whether or not use MTOM for attachments";
    private static final String BROADCAST = "broadcast";
    private static final String BROADCAST_DESCRIPTION = "consumes many services";
    private static final String CALLBACK = "callback";
    private static final String CALLBACK_DESCRIPTION = "async callback";

    private Extension extension;

    private MuleExtensionBuilder populatedBuilder()
    {
        MuleExtensionBuilder builder = DefaultMuleExtensionBuilder.newBuilder();
        return builder.setName(WS_CONSUMER)
                .setDescription(WS_CONSUMER_DESCRIPTION)
                .setVersion(VERSION)
                .setExtensionType(EXTENSION_TYPE)
                .setMinMuleVersion(MIN_MULE_VERSION)
                .addCapablity(TestCapability.class, new TestCapability())
                .addConfiguration(
                        builder.newConfiguration()
                                .addParameter(builder.newParameter()
                                                      .setName(WSDL_LOCATION)
                                                      .setDescription(URI_TO_FIND_THE_WSDL)
                                                      .setRequired(true)
                                                      .setDynamic(false)
                                                      .setType(String.class)
                                )
                                .addParameter(builder.newParameter()
                                                      .setName(SERVICE)
                                                      .setDescription(SERVICE_NAME)
                                                      .setRequired(true)
                                                      .setType(String.class)
                                )
                                .addParameter(builder.newParameter()
                                                      .setName(PORT)
                                                      .setDescription(SERVICE_PORT)
                                                      .setRequired(true)
                                                      .setType(String.class)
                                )
                                .addParameter(builder.newParameter()
                                                      .setName(ADDRESS)
                                                      .setDescription(SERVICE_ADDRESS)
                                                      .setRequired(true)
                                                      .setType(String.class)
                                )
                )
                .addOperation(builder.newOperation()
                                      .setName(CONSUMER)
                                      .setDescription(GO_GET_THEM_TIGER)
                                      .setInputTypes(String.class)
                                      .setOutputTypes(String.class)
                                      .addParameter(builder.newParameter()
                                                            .setName(OPERATION)
                                                            .setDescription(THE_OPERATION_TO_USE)
                                                            .setRequired(true)
                                                            .setType(String.class)
                                      )
                                      .addParameter(builder.newParameter()
                                                            .setName(MTOM_ENABLED)
                                                            .setDescription(MTOM_DESCRIPTION)
                                                            .setRequired(false)
                                                            .setDefaultValue(true)
                                                            .setType(Boolean.class)
                                      )
                ).addOperation(builder.newScope()
                                       .setName(BROADCAST)
                                       .setDescription(BROADCAST_DESCRIPTION)
                                       .setInputTypes(String.class)
                                       .setOutputTypes(List.class)
                                       .addParameter(builder.newParameter()
                                                             .setName(OPERATION)
                                                             .setDescription(THE_OPERATION_TO_USE)
                                                             .setRequired(true)
                                                             .setType(List.class)
                                       ).addParameter(builder.newParameter()
                                                              .setName(MTOM_ENABLED)
                                                              .setDescription(MTOM_DESCRIPTION)
                                                              .setRequired(false)
                                                              .setDefaultValue(true)
                                                              .setType(Boolean.class)
                                       ).addOperationGroup(builder.newOperationGroup()
                                                                   .setName(CALLBACK)
                                                                   .setDescription(CALLBACK_DESCRIPTION)
                                                                   .setAllowedChildsType(MuleExtensionOperationGroup.AllowedChildsType.ANY)
                                                                   .setMinOperations(1)
                                       )
                );
    }

    @Before
    public void buildExtension() throws Exception
    {
        extension = populatedBuilder().build();
    }

    @Test
    public void assertExtension()
    {
        assertEquals(WS_CONSUMER, extension.getName());
        assertEquals(WS_CONSUMER_DESCRIPTION, extension.getDescription());
        assertEquals(VERSION, extension.getVersion());
        assertEquals(EXTENSION_TYPE, extension.getExtensionType());
        assertEquals(MIN_MULE_VERSION, extension.getMinMuleVersion());
        assertEquals(1, extension.getConfigurations().size());

        Optional<TestCapability> capability = extension.getCapability(TestCapability.class);
        assertTrue(capability.isPresent());
        assertTrue(capability.get() instanceof TestCapability);
    }

    @Test
    public void defaultConfiguration() throws Exception
    {
        ExtensionConfiguration configuration = extension.getConfiguration(ExtensionConfiguration.DEFAULT_NAME);
        assertNotNull(configuration);
        assertEquals(ExtensionConfiguration.DEFAULT_NAME, configuration.getName());
        assertEquals(ExtensionConfiguration.DEFAULT_DESCRIPTION, configuration.getDescription());

        List<MuleExtensionParameter> parameters = configuration.getParameters();
        assertEquals(4, parameters.size());
        assertParameter(parameters.get(0), WSDL_LOCATION, URI_TO_FIND_THE_WSDL, false, true, String.class, null);
        assertParameter(parameters.get(1), SERVICE, SERVICE_NAME, true, true, String.class, null);
        assertParameter(parameters.get(2), PORT, SERVICE_PORT, true, true, String.class, null);
        assertParameter(parameters.get(3), ADDRESS, SERVICE_ADDRESS, true, true, String.class, null);
    }

    @Test
    public void onlyOneConfig() throws Exception
    {
        assertEquals(1, extension.getConfigurations().size());
        assertSame(extension.getConfigurations().get(0), extension.getConfiguration(ExtensionConfiguration.DEFAULT_NAME));
    }

    @Test(expected = NoSuchConfigurationException.class)
    public void noSuchConfiguration() throws Exception
    {
        extension.getConfiguration("fake");
    }

    @Test(expected = NoSuchOperationException.class)
    public void noSuchOperation() throws Exception
    {
        extension.getOperation("fake");
    }

    @Test
    public void noSuchCapability()
    {
        Optional<Capability> capability = extension.getCapability(Capability.class);
        assertFalse(capability.isPresent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCapabilityType()
    {
        populatedBuilder().addCapablity(null, new TestCapability());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullCapability()
    {
        populatedBuilder().addCapablity(TestCapability.class, null);
    }

    @Test(expected = IllegalStateException.class)
    public void invalidMinMuleVersion() throws Exception
    {
        populatedBuilder().setMinMuleVersion("3.5.0").build();
    }

    @Test
    public void operations() throws Exception
    {
        List<ExtensionOperation> operations = extension.getOperations();
        assertEquals(2, operations.size());
        assertConsumeOperation(operations);
        assertBroadcastOperation(operations);
    }

    private void assertConsumeOperation(List<ExtensionOperation> operations) throws NoSuchOperationException
    {
        ExtensionOperation operation = operations.get(0);
        assertSame(operation, extension.getOperation(CONSUMER));

        assertEquals(CONSUMER, operation.getName());
        assertEquals(GO_GET_THEM_TIGER, operation.getDescription());
        strictTypeAssert(operation.getInputTypes(), String.class);
        strictTypeAssert(operation.getOutputTypes(), String.class);

        List<MuleExtensionParameter> parameters = operation.getParameters();
        assertEquals(2, parameters.size());
        assertParameter(parameters.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, String.class, null);
        assertParameter(parameters.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, Boolean.class, true);
    }

    private void assertBroadcastOperation(List<ExtensionOperation> operations) throws NoSuchOperationException
    {
        ExtensionOperation operation = operations.get(1);
        assertSame(operation, extension.getOperation(BROADCAST));

        assertEquals(BROADCAST, operation.getName());
        assertEquals(BROADCAST_DESCRIPTION, operation.getDescription());
        strictTypeAssert(operation.getInputTypes(), String.class);
        strictTypeAssert(operation.getOutputTypes(), List.class);

        List<MuleExtensionParameter> parameters = operation.getParameters();
        assertEquals(2, parameters.size());
        assertParameter(parameters.get(0), OPERATION, THE_OPERATION_TO_USE, true, true, List.class, null);
        assertParameter(parameters.get(1), MTOM_ENABLED, MTOM_DESCRIPTION, true, false, Boolean.class, true);

        assertTrue(operation instanceof ExtensionScope);
        ExtensionScope scope = (ExtensionScope) operation;
        assertEquals(1, scope.getGroups().size());
        MuleExtensionOperationGroup group = scope.getGroups().get(0);
        assertEquals(CALLBACK, group.getName());
        assertEquals(CALLBACK_DESCRIPTION, group.getDescription());
        assertEquals(1, group.getMinOperations());
        assertEquals(0, group.getMaxOperations());
        assertEquals(MuleExtensionOperationGroup.AllowedChildsType.ANY, group.getAllowedChildsType());
    }

    private void assertParameter(MuleExtensionParameter parameter,
                                 String name,
                                 String description,
                                 boolean acceptsExpressions,
                                 boolean required,
                                 Class<?> type,
                                 Object defaultValue)
    {

        assertNotNull(parameter);
        assertEquals(name, parameter.getName());
        assertEquals(description, parameter.getDescription());
        assertEquals(acceptsExpressions, parameter.isDynamic());
        assertEquals(required, parameter.isRequired());
        assertEquals(type, parameter.getType());

        if (defaultValue != null)
        {
            assertEquals(defaultValue, parameter.getDefaultValue());
        }
        else
        {
            assertNull(parameter.getDefaultValue());
        }
    }

    private void strictTypeAssert(List<Class<?>> types, Class<?> expected)
    {
        assertEquals(1, types.size());
        assertEquals(expected, types.get(0));
    }

    private class TestCapability
    {

        @Override
        public String getName()
        {
            return "test";
        }

        @Override
        public String getDescription()
        {
            return "test capability";
        }
    }

}

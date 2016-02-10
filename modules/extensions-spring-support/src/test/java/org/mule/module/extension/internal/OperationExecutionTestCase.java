/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.module.extension.HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER;
import static org.mule.module.extension.model.HealthStatus.DEAD;
import static org.mule.module.extension.model.KnockeableDoor.knock;
import static org.mule.module.extension.model.Ricin.RICIN_KILL_MESSAGE;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.connection.ConnectionException;
import org.mule.extension.api.ExtensionManager;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.extension.HeisenbergConnectionProvider;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.HeisenbergOperations;
import org.mule.module.extension.exception.HeisenbergException;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.module.extension.model.KnockeableDoor;
import org.mule.module.extension.model.Ricin;
import org.mule.module.extension.model.Weapon;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class OperationExecutionTestCase extends ExtensionFunctionalTestCase
{

    private static final String GUSTAVO_FRING = "Gustavo Fring";
    private static final BigDecimal MONEY = BigDecimal.valueOf(1000000);
    private static final String GOODBYE_MESSAGE = "Say hello to my little friend";
    private static final String VICTIM = "Skyler";
    public static final String HEISENBERG = "heisenberg";
    public static final long PAYMENT = 100;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-operation-config.xml";
    }

    @Test
    public void operationWithReturnValueAndWithoutParameters() throws Exception
    {
        runFlowAndExpect("sayMyName", HEISENBERG);
    }

    @Test
    public void operationWithReturnValueOnTarget() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = spy(event.getMessage());
        event.setMessage(message);
        event = spy(event);

        MuleEvent responseEvent = runFlow("sayMyNameOnTarget", event);

        assertThat(responseEvent.getMessage().getPayload(), is(""));
        verify(event, never()).setMessage(any(MuleMessage.class));
        verify(message, never()).setPayload(any(Object.class));

        MuleMessage responseMessage = event.getFlowVariable("myFace");
        assertThat(responseMessage.getPayload(), is(HEISENBERG));
    }

    @Test
    public void voidOperationWithoutParameters() throws Exception
    {
        MuleEvent originalEvent = getTestEvent(EMPTY);
        MuleEvent responseEvent = runFlow("die", originalEvent);
        assertThat(responseEvent.getMessageAsString(), is(EMPTY));
        assertThat(getConfig(HEISENBERG).getEndingHealth(), is(DEAD));
    }

    @Test
    public void operationWithFixedParameter() throws Exception
    {
        runFlowAndExpect("getFixedEnemy", GUSTAVO_FRING);
    }

    @Test
    public void operationWithDefaulValueParameter() throws Exception
    {
        runFlowAndExpect("getDefaultEnemy", GUSTAVO_FRING);
    }

    @Test
    public void operationWithDynamicParameter() throws Exception
    {
        doTestExpressionEnemy(0);
    }

    @Test
    public void operationWithTransformedParameter() throws Exception
    {
        doTestExpressionEnemy("0");
    }

    @Test
    public void operationWithInjectedEvent() throws Exception
    {
        runFlow("collectFromEvent", getTestEvent(Long.valueOf(PAYMENT)));
        assertThat(getConfig(HEISENBERG).getMoney(), is(MONEY.add(BigDecimal.valueOf(PAYMENT))));
    }

    @Test
    public void operationWithInjectedMessage() throws Exception
    {
        runFlow("collectFromMessage", getTestEvent(Long.valueOf(PAYMENT)));
        assertThat(getConfig(HEISENBERG).getMoney(), is(MONEY.add(BigDecimal.valueOf(PAYMENT))));
    }

    @Test
    public void parameterFixedAtPayload() throws Exception
    {
        assertKillByPayload("killFromPayload");
    }

    @Test
    public void optionalParameterDefaultingToPayload() throws Exception
    {
        assertKillByPayload("customKillWithDefault");
    }

    @Test
    public void optionalParameterWithDefaultOverride() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("goodbye", GOODBYE_MESSAGE);
        event.setFlowVariable("victim", VICTIM);
        event = runFlow("customKillWithoutDefault", event);
        assertKillPayload(event);
    }

    @Test
    public void oneNestedOperation() throws Exception
    {
        MuleEvent event = runFlow("killOne");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void manyNestedOperations() throws Exception
    {
        MuleEvent event = runFlow("killMany");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n" +
                          "bye bye, Frank\n" +
                          "bye bye, Nazi dudes\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void manyNestedOperationsSupportedButOnlyOneProvided() throws Exception
    {
        MuleEvent event = runFlow("killManyButOnlyOneProvided");
        String expected = "Killed the following because I'm the one who knocks:\n" +
                          "bye bye, Gustavo Fring\n";

        assertThat(expected, is(event.getMessageAsString()));
    }

    @Test
    public void getInjectedDependency() throws Exception
    {
        ExtensionManager extensionManager = (ExtensionManager) runFlow("injectedExtensionManager").getMessage().getPayload();
        assertThat(extensionManager, is(sameInstance(muleContext.getExtensionManager())));
    }

    @Test
    public void alias() throws Exception
    {
        String alias = runFlow("alias").getMessageAsString();
        assertThat(alias, is("Howdy!, my name is Walter White and I'm 52 years old"));
    }

    @Test
    public void operationWithStaticInlinePojoParameter() throws Exception
    {
        String response = getPayloadAsString(runFlow("knockStaticInlineDoor").getMessage());
        assertKnockedDoor(response, "Inline Skyler");
    }

    @Test
    public void operationWithDynamicInlinePojoParameter() throws Exception
    {
        assertDynamicDoor("knockDynamicInlineDoor");
    }

    @Test
    public void operationWithStaticTopLevelPojoParameter() throws Exception
    {
        String response = getPayloadAsString(runFlow("knockStaticTopLevelDoor").getMessage());
        assertKnockedDoor(response, "Top Level Skyler");
    }

    @Test
    public void operationWithDynamicTopLevelPojoParameter() throws Exception
    {
        assertDynamicDoor("knockDynamicTopLevelDoor");
    }

    @Test
    public void operationWithInlineListParameter() throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("victim", "Saul");

        List<String> response = (List<String>) runFlow("knockManyWithInlineList", event).getMessage().getPayload();
        assertThat(response, Matchers.contains(knock("Inline Skyler"), knock("Saul")));
    }

    @Test
    public void operationWithExpressionListParameter() throws Exception
    {
        List<KnockeableDoor> doors = Arrays.asList(new KnockeableDoor("Skyler"), new KnockeableDoor("Saul"));
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("doors", doors);

        List<String> response = (List<String>) runFlow("knockManyByExpression", event).getMessage().getPayload();
        assertThat(response, Matchers.contains(knock("Skyler"), knock("Saul")));
    }

    @Test
    public void operationWhichRequiresConnection() throws Exception
    {
        assertThat(getPayloadAsString(runFlow("callSaul").getMessage()), is("You called " + SAUL_OFFICE_NUMBER));
    }

    @Test
    public void extensionWithExceptionEnricher() throws Throwable
    {
        expectedException.expect(ConnectionException.class);
        expectedException.expectMessage(is(HeisenbergOperations.CALL_GUS_MESSAGE));
        runFlowAndThrowCause("callGus");
    }

    @Test
    public void operationWithExceptionEnricher() throws Throwable
    {
        expectedException.expect(HeisenbergException.class);
        expectedException.expectMessage(is(HeisenbergOperations.CURE_CANCER_MESSAGE));
        runFlowAndThrowCause("cureCancer");
    }

    private void runFlowAndThrowCause(String callGus) throws Throwable
    {
        try
        {
            runFlow(callGus);
            fail("An exception was expected");
        }
        catch (MuleException me)
        {
            throw me.getCause();
        }
    }

    @Test
    public void operationWhichConsumesANonInstantiableArgument() throws Exception
    {
        MuleEvent event = getTestEvent(EMPTY);
        Ricin ricinWeapon = new Ricin();
        ricinWeapon.setMicrogramsPerKilo(10L);
        event.setFlowVariable("weapon", ricinWeapon);

        event = runFlow("killWithWeapon", event);
        assertThat(event.getMessageAsString(), is(RICIN_KILL_MESSAGE));
    }


    @Test
    public void connectionProviderDefaultValueSaulPhoneNumber() throws Exception
    {
        MuleEvent getSaulNumber = runFlow("getSaulNumber");
        assertThat(getSaulNumber.getMessage().getPayload(), is(HeisenbergConnectionProvider.SAUL_OFFICE_NUMBER));
    }

    @Test
    public void operationWhichConsumesAListOfNonInstantiableArgument() throws Exception
    {
        MuleEvent event = getTestEvent(EMPTY);
        Ricin ricinWeapon1 = new Ricin();
        ricinWeapon1.setMicrogramsPerKilo(10L);
        Ricin ricinWeapon2 = new Ricin();
        ricinWeapon2.setMicrogramsPerKilo(10L);

        List<Weapon> weaponList = Arrays.asList(ricinWeapon1, ricinWeapon2);
        event.setFlowVariable("weapons", weaponList);

        event = runFlow("killWithMultipleWeapons", event);
        List<String> result = weaponList.stream().map(Weapon::kill).collect(Collectors.toList());
        assertThat(event.getMessage().getPayload(), is(result));
    }

    @Test
    public void operationWithLiteralArgument() throws Exception
    {
        MuleEvent event = getTestEvent(EMPTY);
        event = runFlow("literalEcho", event);
        assertThat(event.getMessage().getPayload(), is("#[money]"));
    }

    private void assertDynamicDoor(String flowName) throws Exception
    {
        assertDynamicVictim(flowName, "Skyler");
        assertDynamicVictim(flowName, "Saul");
    }

    private void assertDynamicVictim(String flowName, String victim) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("victim", victim);

        assertKnockedDoor(getPayloadAsString(runFlow(flowName, event).getMessage()), victim);
    }

    private void assertKnockedDoor(String actual, String expected)
    {
        assertThat(actual, is(knock(expected)));
    }

    private void assertKillPayload(MuleEvent event) throws MuleException
    {
        assertThat(event.getMessageAsString(), is(String.format("%s, %s", GOODBYE_MESSAGE, VICTIM)));
    }

    private void assertKillByPayload(String flowName) throws Exception
    {
        MuleEvent event = getTestEvent(VICTIM);
        event.setFlowVariable("goodbye", GOODBYE_MESSAGE);
        event = runFlow(flowName, event);
        assertKillPayload(event);
    }

    private void doTestExpressionEnemy(Object enemyIndex) throws Exception
    {
        MuleEvent event = getTestEvent(EMPTY);
        event.setFlowVariable("enemy", enemyIndex);
        event = runFlow("expressionEnemy", event);
        assertThat(event.getMessageAsString(), is(GUSTAVO_FRING));
    }

    private HeisenbergExtension getConfig(String name) throws Exception
    {
        return ExtensionsTestUtils.getConfigurationFromRegistry(name, getTestEvent(""));
    }
}

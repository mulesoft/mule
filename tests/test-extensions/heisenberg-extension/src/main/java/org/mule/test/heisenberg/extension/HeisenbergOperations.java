/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.temporary.MuleMessage;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.annotation.DataTypeParameters;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.RestrictedTo;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Ignore;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport;
import org.mule.test.heisenberg.extension.exception.CureCancerExceptionEnricher;
import org.mule.test.heisenberg.extension.exception.HealthException;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.HealthStatus;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class HeisenbergOperations
{

    public static final String CURE_CANCER_MESSAGE = "Can't help you, you are going to die";
    public static final String CALL_GUS_MESSAGE = "You are not allowed to speak with gus.";
    public static final String KILL_WITH_GROUP = "KillGroup";

    public static final String OPERATION_WITH_DISPLAY_NAME_PARAMETER = "literalEcho";
    public static final String OPERATION_PARAMETER_ORIGINAL_OVERRIDED_DISPLAY_NAME = "literalExpression";
    public static final String OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME = "Custom overrided display name";

    @Inject
    private ExtensionManager extensionManager;

    @DataTypeParameters
    public String sayMyName(@UseConfig HeisenbergExtension config)
    {
        return config.getPersonalInfo().getName();
    }

    public void die(@UseConfig HeisenbergExtension config)
    {
        config.setEndingHealth(HealthStatus.DEAD);
    }

    public MuleMessage<String, Integer> getEnemy(@UseConfig HeisenbergExtension config, @Optional(defaultValue = "0") int index)
    {
        org.mule.runtime.api.metadata.DataType<String> dt = DataTypeFactory.create(String.class);
        Charset lastSupportedEncoding = Charset.availableCharsets().values().stream().reduce((first, last) -> last).get();
        dt.setEncoding(lastSupportedEncoding.toString());
        dt.setMimeType("dead/dead");

        MuleMessage message = new DefaultMuleMessage(config.getEnemies().get(index), dt, index);
        return (MuleMessage<String, Integer>) message;
    }

    public String kill(@Optional(defaultValue = "#[payload]") String victim, String goodbyeMessage) throws Exception
    {
        return killWithCustomMessage(victim, goodbyeMessage);
    }

    public String killWithCustomMessage(@Optional(defaultValue = "#[payload]") @Placement(group = KILL_WITH_GROUP, order = 1) String victim,
                                        @Placement(group = KILL_WITH_GROUP, order = 2) String goodbyeMessage)
    {
        return String.format("%s, %s", goodbyeMessage, victim);
    }

    public String knock(KnockeableDoor door)
    {
        return door.knock();
    }

    public String killWithWeapon(Weapon weapon, Weapon.WeaponType type, Weapon.WeaponAttributes weaponAttributes)
    {
        return String.format("Killed with: %s , Type %s and attribute %s", weapon.kill(), type.name(), weaponAttributes.getBrand());
    }

    public List<String> killWithMultiplesWeapons(List<Weapon> weaponList)
    {
        return weaponList.stream().map(Weapon::kill).collect(Collectors.toList());
    }

    public List<String> killWithMultipleWildCardWeapons(List<? extends Weapon> wildCardWeapons)
    {
        return wildCardWeapons.stream().map(Weapon::kill).collect(Collectors.toList());
    }

    public String killMany(@RestrictedTo(HeisenbergExtension.class) List<NestedProcessor> killOperations, String reason) throws Exception
    {
        StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
        for (NestedProcessor processor : killOperations)
        {
            builder.append(processor.process()).append("\n");
        }

        return builder.toString();
    }

    public String killOne(@RestrictedTo(HeisenbergExtension.class) NestedProcessor killOperation, String reason) throws Exception
    {
        StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
        builder.append(killOperation.process()).append("\n");

        return builder.toString();
    }

    public ExtensionManager getInjectedExtensionManager()
    {
        return extensionManager;
    }

    public void getPaymentFromEvent(@UseConfig HeisenbergExtension config, MuleEvent event)
    {
        Long payment = (Long) event.getMessage().getPayload();
        config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
    }

    public String alias(String greeting, @ParameterGroup PersonalInfo info)
    {
        return String.format("%s, my name is %s and I'm %d years old", greeting, info.getName(), info.getAge());
    }

    public void getPaymentFromMessage(@UseConfig HeisenbergExtension config, MuleMessage<Long, Serializable> message)
    {
        Long payment = message.getPayload();
        config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
    }

    public List<String> knockMany(List<KnockeableDoor> doors)
    {
        return doors.stream().map(KnockeableDoor::knock).collect(toList());
    }

    public String callSaul(@Connection HeisenbergConnection connection)
    {
        return connection.callSaul();
    }

    public String callGusFring() throws HeisenbergException
    {
        throw new HeisenbergException(CALL_GUS_MESSAGE);
    }

    @OnException(CureCancerExceptionEnricher.class)
    public String cureCancer() throws HealthException
    {
        throw new HealthException(CURE_CANCER_MESSAGE);
    }

    public String getSaulPhone(@Connection HeisenbergConnection connection)
    {
        return connection.getSaulPhoneNumber();
    }

    public String literalEcho(@DisplayName(OPERATION_PARAMETER_OVERRIDED_DISPLAY_NAME) @Expression(ExpressionSupport.LITERAL) String literalExpression)
    {
        return literalExpression;
    }

    @Ignore
    public void ignoredOperation()
    {

    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import static java.util.stream.Collectors.toList;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.NestedProcessor;
import org.mule.api.temporary.MuleMessage;
import org.mule.extension.annotation.api.DataTypeParameters;
import org.mule.extension.annotation.api.Expression;
import org.mule.extension.annotation.api.OnException;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.RestrictedTo;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.introspection.ExpressionSupport;
import org.mule.module.extension.exception.CureCancerExceptionEnricher;
import org.mule.module.extension.exception.HealthException;
import org.mule.module.extension.exception.HeisenbergException;
import org.mule.module.extension.model.HealthStatus;
import org.mule.module.extension.model.KnockeableDoor;
import org.mule.module.extension.model.PersonalInfo;
import org.mule.module.extension.model.Weapon;
import org.mule.transformer.types.DataTypeFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class HeisenbergOperations
{

    private static final String SECRET_PACKAGE = "secretPackage";
    private static final String CONTENT_TYPE = "contentType";
    private static final String METH = "meth";
    public static final String CURE_CANCER_MESSAGE = "Can't help you, you are going to die";
    public static final String CALL_GUS_MESSAGE = "You are not allowed to speak with gus.";

    @Inject
    private ExtensionManager extensionManager;

    @Operation
    @DataTypeParameters
    public String sayMyName(@UseConfig HeisenbergExtension config)
    {
        return config.getPersonalInfo().getName();
    }

    @Operation
    public void die(@UseConfig HeisenbergExtension config)
    {
        config.setEndingHealth(HealthStatus.DEAD);
    }

    @Operation
    public MuleMessage getEnemy(@UseConfig HeisenbergExtension config, @Optional(defaultValue = "0") int index)
    {
        org.mule.api.metadata.DataType<String> dt = DataTypeFactory.create(String.class);
        Charset lastSupportedEncoding = Charset.availableCharsets().values().stream().reduce((first, last) -> last).get();
        dt.setEncoding(lastSupportedEncoding.toString());
        dt.setMimeType("dead/dead");
        return new DefaultMuleMessage(config.getEnemies().get(index), dt);
    }

    @Operation
    public String kill(@Optional(defaultValue = "#[payload]") String victim, String goodbyeMessage) throws Exception
    {
        return killWithCustomMessage(victim, goodbyeMessage);
    }

    @Operation
    public String killWithCustomMessage(@Optional(defaultValue = "#[payload]") String victim, String goodbyeMessage)
    {
        return String.format("%s, %s", goodbyeMessage, victim);
    }

    @Operation
    public String knock(KnockeableDoor door)
    {
        return door.knock();
    }

    @Operation
    public String killWithWeapon(Weapon weapon)
    {
        return weapon.kill();
    }

    @Operation
    public List<String> killWithMultiplesWeapons(List<Weapon> weaponList)
    {
        return weaponList.stream().map(Weapon::kill).collect(Collectors.toList());
    }

    @Operation
    public List<String> killWithMultipleWildCardWeapons(List<? extends Weapon> wildCardWeapons)
    {
        return wildCardWeapons.stream().map(Weapon::kill).collect(Collectors.toList());
    }

    @Operation
    public String killMany(@RestrictedTo(HeisenbergExtension.class) List<NestedProcessor> killOperations, String reason) throws Exception
    {
        StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
        for (NestedProcessor processor : killOperations)
        {
            builder.append(processor.process()).append("\n");
        }

        return builder.toString();
    }

    @Operation
    public String killOne(@RestrictedTo(HeisenbergExtension.class) NestedProcessor killOperation, String reason) throws Exception
    {
        StringBuilder builder = new StringBuilder("Killed the following because " + reason + ":\n");
        builder.append(killOperation.process()).append("\n");

        return builder.toString();
    }

    @Operation
    public ExtensionManager getInjectedExtensionManager()
    {
        return extensionManager;
    }

    @Operation
    public void getPaymentFromEvent(@UseConfig HeisenbergExtension config, MuleEvent event)
    {
        Long payment = (Long) event.getMessage().getPayload();
        config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
    }

    @Operation
    public String alias(String greeting, @ParameterGroup PersonalInfo info)
    {
        return String.format("%s, my name is %s and I'm %d years old", greeting, info.getName(), info.getAge());
    }

    @Operation
    public void getPaymentFromMessage(@UseConfig HeisenbergExtension config, MuleMessage<Long, Serializable> message)
    {
        Long payment = message.getPayload();
        config.setMoney(config.getMoney().add(BigDecimal.valueOf(payment)));
    }

    @Operation
    public List<String> knockMany(List<KnockeableDoor> doors)
    {
        return doors.stream().map(KnockeableDoor::knock).collect(toList());
    }

    @Operation
    public String callSaul(@Connection HeisenbergConnection connection)
    {
        return connection.callSaul();
    }

    @Operation
    public String callGusFring() throws HeisenbergException
    {
        throw new HeisenbergException(CALL_GUS_MESSAGE);
    }

    @Operation
    @OnException(CureCancerExceptionEnricher.class)
    public String cureCancer() throws HealthException
    {
        throw new HealthException(CURE_CANCER_MESSAGE);
    }

    @Operation
    public String getSaulPhone(@Connection HeisenbergConnection connection)
    {
        return connection.getSaulPhoneNumber();
    }

    @Operation
    public String literalEcho(@Expression(ExpressionSupport.LITERAL) String literalExpression)
    {
        return literalExpression;
    }
}

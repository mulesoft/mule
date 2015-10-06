/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.extension.annotation.api.ExposeContentType;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.RestrictedTo;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.runtime.ContentType;

import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

public class HeisenbergOperations
{

    private static final String SECRET_PACKAGE = "secretPackage";
    private static final String METH = "meth";

    @Inject
    private ExtensionManager extensionManager;

    @Operation
    @ExposeContentType
    public String sayMyName(@UseConfig HeisenbergExtension config, ContentType contentType)
    {
        return config.getPersonalInfo().getName();
    }

    @Operation
    public void die(@UseConfig HeisenbergExtension config, ContentType contentType)
    {
        config.setEndingHealth(HealthStatus.DEAD);
        contentType.setMimeType("dead/dead");
        Charset lastSupportedEncoding = Charset.availableCharsets().values().stream().reduce((first, last) -> last).get();
        contentType.setEncoding(lastSupportedEncoding);
    }

    @Operation
    public String getEnemy(@UseConfig HeisenbergExtension config, int index)
    {
        return config.getEnemies().get(index);
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
    public void hideMethInEvent(MuleEvent event)
    {
        event.setFlowVariable(SECRET_PACKAGE, METH);
    }

    @Operation
    public String alias(String greeting, @ParameterGroup PersonalInfo info)
    {
        return String.format("%s, my name is %s and I'm %d years old", greeting, info.getName(), info.getAge());
    }

    @Operation
    public void hideMethInMessage(MuleMessage message)
    {
        message.setProperty(SECRET_PACKAGE, METH, PropertyScope.INVOCATION);
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import static java.util.stream.Collectors.toList;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.extension.annotation.api.ContentMetadataParameters;
import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.ParameterGroup;
import org.mule.extension.annotation.api.RestrictedTo;
import org.mule.extension.annotation.api.param.Connection;
import org.mule.extension.annotation.api.param.Optional;
import org.mule.extension.annotation.api.param.UseConfig;
import org.mule.extension.api.ExtensionManager;
import org.mule.extension.api.runtime.ContentMetadata;
import org.mule.extension.api.runtime.ContentType;

import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

public class HeisenbergOperations
{

    private static final String SECRET_PACKAGE = "secretPackage";
    private static final String CONTENT_TYPE = "contentType";
    private static final String METH = "meth";

    @Inject
    private ExtensionManager extensionManager;

    @Operation
    @ContentMetadataParameters
    public String sayMyName(@UseConfig HeisenbergExtension config, ContentMetadata contentMetadata)
    {
        return config.getPersonalInfo().getName();
    }

    @Operation
    public void die(@UseConfig HeisenbergExtension config)
    {
        config.setEndingHealth(HealthStatus.DEAD);
    }

    @Operation
    public String getEnemy(@UseConfig HeisenbergExtension config, int index, ContentMetadata contentMetadata)
    {
        if (contentMetadata.isOutputModifiable())
        {
            Charset lastSupportedEncoding = Charset.availableCharsets().values().stream().reduce((first, last) -> last).get();
            contentMetadata.setOutputContentType(new ContentType(lastSupportedEncoding, "dead/dead"));
        }

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
    public void hideMethInEvent(MuleEvent event, ContentType contentType)
    {
        event.setFlowVariable(SECRET_PACKAGE, METH);
        event.setFlowVariable(CONTENT_TYPE, contentType);
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

    @Operation
    public String knock(KnockeableDoor door)
    {
        return door.knock();
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
}

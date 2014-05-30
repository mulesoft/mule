/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.extensions.api.annotation.Configurable;
import org.mule.extensions.api.annotation.Module;
import org.mule.extensions.api.annotation.Operation;
import org.mule.extensions.api.annotation.param.Optional;
import org.mule.extensions.api.annotation.param.Payload;
import org.mule.extensions.spi.NestedProcessor;

import java.util.LinkedList;
import java.util.List;

@Module(name = HeisenbergModule.EXTENSION_NAME, description = HeisenbergModule.EXTENSION_DESCRIPTION, version = HeisenbergModule.EXTENSION_VERSION)
public class HeisenbergModule
{

    public static final String HEISENBERG = "Heisenberg";
    public static final String EXTENSION_NAME = "HeisenbergExtension";
    public static final String EXTENSION_DESCRIPTION = "My Test Extension just to unit test";
    public static final String EXTENSION_VERSION = "1.0";

    @Configurable
    @Optional(defaultValue = HEISENBERG)
    private String name;

    @Configurable
    private List<String> enemies = new LinkedList<String>();

    @Operation
    public String sayMyName()
    {
        return name;
    }

    @Operation
    public String getEnemy(int index)
    {
        return enemies.get(index);
    }

    @Operation
    public String kill(@Payload String goodbyeMessage,
                       NestedProcessor enemiesLookup) throws Exception
    {

        return killWithCustomMessage(goodbyeMessage, enemiesLookup);
    }

    //TODO: Be able to parse this as a scope
    @Operation
    public String killWithCustomMessage(@Optional(defaultValue = "#[payload]") String goodbyeMessage,
                                        NestedProcessor enemiesLookup) throws Exception
    {
        List<String> toKill = (List<String>) enemiesLookup.process();
        StringBuilder builder = new StringBuilder();

        for (String kill : toKill)
        {
            builder.append(String.format("%s: %s", goodbyeMessage, kill)).append("\n");
        }

        return builder.toString();
    }

    @Operation
    public void hideMethInEvent(MuleEvent event)
    {
        hideMethInMessage(event.getMessage());
    }

    @Operation
    public void hideMethInMessage(MuleMessage message)
    {
        message.setProperty("secretPackage", "meth", PropertyScope.INVOCATION);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<String> getEnemies()
    {
        return enemies;
    }

    public void setEnemies(List<String> enemies)
    {
        this.enemies = enemies;
    }
}

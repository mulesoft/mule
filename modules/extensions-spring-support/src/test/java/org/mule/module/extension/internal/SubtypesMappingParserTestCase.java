/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.extension.internal.runtime.connector.subtypes.CarDoor;
import org.mule.module.extension.internal.runtime.connector.subtypes.FinalPojo;
import org.mule.module.extension.internal.runtime.connector.subtypes.HouseDoor;
import org.mule.module.extension.internal.runtime.connector.subtypes.Square;
import org.mule.module.extension.internal.runtime.connector.subtypes.SubTypesMappingConnector;
import org.mule.module.extension.internal.runtime.connector.subtypes.SubtypesConnectorConnection;
import org.mule.module.extension.internal.runtime.connector.subtypes.Triangle;

import java.util.List;

import org.junit.Test;

public class SubTypesMappingParserTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {SubTypesMappingConnector.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "subtypes-mapping.xml";
    }

    @Test
    public void shapeRetriever() throws Exception
    {
        MuleEvent responseEvent = flowRunner("shapeRetriever").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), instanceOf(Square.class));

        Square payload = (Square) responseEvent.getMessage().getPayload();
        assertThat(payload.getSide(), is(4));
        assertThat(payload.getArea(), is(16));
    }

    @Test
    public void doorRetriever() throws Exception
    {
        MuleEvent responseEvent = flowRunner("doorRetriever").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), instanceOf(CarDoor.class));

        CarDoor payload = (CarDoor) responseEvent.getMessage().getPayload();
        assertThat(payload.getColor(), is("blue"));
    }

    @Test
    public void configRetriever() throws Exception
    {
        MuleEvent responseEvent = flowRunner("configRetriever").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), instanceOf(SubTypesMappingConnector.class));

        SubTypesMappingConnector payload = (SubTypesMappingConnector) responseEvent.getMessage().getPayload();
        assertThat(payload.getAbstractShape(), instanceOf(Square.class));
        assertThat(payload.getAbstractShape().getArea(), is(1));

        assertThat(payload.getExplicitSquare(), instanceOf(Square.class));
        assertThat(payload.getExplicitSquare().getArea(), is(4));

        assertThat(payload.getDoorInterface(), instanceOf(CarDoor.class));
        assertThat(payload.getFinalPojo(), instanceOf(FinalPojo.class));
    }


    @Test
    public void connectionRetriever() throws Exception
    {
        MuleEvent responseEvent = flowRunner("connectionRetriever").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), instanceOf(SubtypesConnectorConnection.class));

        SubtypesConnectorConnection payload = (SubtypesConnectorConnection) responseEvent.getMessage().getPayload();
        assertThat(payload.getDoor(), instanceOf(HouseDoor.class));
        assertThat(payload.getShape(), instanceOf(Triangle.class));
        assertThat(payload.getShape().getArea(), is(1));
    }

    @Test
    public void subtypedAndConcreteParameters() throws Exception
    {
        MuleEvent responseEvent = flowRunner("subtypedAndConcreteParameters").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), notNullValue());

        List<Object> payload = (List<Object>) responseEvent.getMessage().getPayload();
        assertThat(payload.get(0), instanceOf(Triangle.class));
        assertThat(((Triangle) payload.get(0)).getHeight(), is(4));
        assertThat(((Triangle) payload.get(0)).getArea(), is(2));

        assertThat(payload.get(1), instanceOf(HouseDoor.class));
        assertThat(((HouseDoor) payload.get(1)).isLocked(), is(false));

        assertThat(payload.get(2), instanceOf(FinalPojo.class));
        assertThat(((FinalPojo) payload.get(2)).getSomeString(), is("asChild"));
    }

    @Test
    public void subtypedAndConcreteParametersAsAttributes() throws Exception
    {
        MuleEvent responseEvent = flowRunner("subtypedAndConcreteParametersAsAttributes").withPayload("").run();

        assertThat(responseEvent.getMessage().getPayload(), notNullValue());

        List<Object> payload = (List<Object>) responseEvent.getMessage().getPayload();
        assertThat(payload.get(0), instanceOf(Square.class));
        assertThat(((Square) payload.get(0)).getSide(), is(3));
        assertThat(((Square) payload.get(0)).getArea(), is(9));

        assertThat(payload.get(1), instanceOf(CarDoor.class));
        assertThat(((CarDoor) payload.get(1)).getColor(), is("white"));

        assertThat(payload.get(2), instanceOf(FinalPojo.class));
        assertThat(((FinalPojo) payload.get(2)).getSomeString(), is("globalString"));
    }
}

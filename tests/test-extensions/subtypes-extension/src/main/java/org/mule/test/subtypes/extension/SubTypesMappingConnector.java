/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.vegan.extension.VeganCookBook;
import org.mule.test.vegan.extension.VeganExtension;

@Extension(name = "SubtypesConnector", description = "Test connector for pojo subtype mapping")
@Operations(TestOperationsWithSubTypeMapping.class)
@Providers(SubTypesConnectionProvider.class)
@SubTypeMapping(baseType = Shape.class, subTypes = {Square.class, Triangle.class})
@SubTypeMapping(baseType = Door.class, subTypes = {HouseDoor.class, CarDoor.class})
@Import(type = Ricin.class, from = HeisenbergExtension.class)
@Import(type = VeganCookBook.class, from = VeganExtension.class)
@Xml(namespace = "subtypes", namespaceLocation = "http://www.mulesoft.org/schema/mule/subtypes")
public class SubTypesMappingConnector
{

    @Parameter
    private Shape abstractShape;

    @Parameter
    private Door doorInterface;

    @Parameter
    private Square explicitSquare;

    @Parameter
    private FinalPojo finalPojo;

    @Parameter
    private Ricin ricin;

    public Shape getAbstractShape()
    {
        return abstractShape;
    }

    public Door getDoorInterface()
    {
        return doorInterface;
    }

    public Square getExplicitSquare()
    {
        return explicitSquare;
    }

    public FinalPojo getFinalPojo()
    {
        return finalPojo;
    }
}

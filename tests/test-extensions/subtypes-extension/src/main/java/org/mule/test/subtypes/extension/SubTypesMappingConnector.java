/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.vegan.extension.VeganCookBook;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.List;

@Extension(name = "SubtypesConnector", description = "Test connector for pojo subtype mapping")
@Operations(TestOperationsWithSubTypeMapping.class)
@Sources(SubtypesSource.class)
@Providers(SubTypesConnectionProvider.class)
@SubTypeMapping(baseType = ParentShape.class, subTypes = {Square.class, Triangle.class})
@SubTypeMapping(baseType = Door.class, subTypes = {HouseDoor.class, CarDoor.class})
@Import(type = Ricin.class, from = HeisenbergExtension.class)
@Import(type = Weapon.class, from = HeisenbergExtension.class)
@Import(type = VeganCookBook.class, from = VeganExtension.class)
@Export(classes = {Revolver.class})
@Xml(namespace = "subtypes", namespaceLocation = "http://www.mulesoft.org/schema/mule/subtypes")
public class SubTypesMappingConnector
{

    @Parameter
    private ParentShape abstractShape;

    @Parameter
    private Door doorInterface;

    @Parameter
    private Square explicitSquare;

    @Parameter
    private FinalPojo finalPojo;

    @Parameter
    private Ricin ricin;

    @Parameter
    private Weapon extensibleWeapon;

    @Parameter
    private ParentShape triangle;

    @Parameter
    private ExtensiblePojo pojoWithExtension;

    @Parameter
    private List<PojoForList> pojoListOne;

    @Parameter
    private List<PojoForList> pojoListTwo;

    public List<PojoForList> getPojoListOne()
    {
        return pojoListOne;
    }

    public List<PojoForList> getPojoListTwo()
    {
        return pojoListTwo;
    }

    public ExtensiblePojo getExtensiblePojo()
    {
        return pojoWithExtension;
    }

    public ParentShape getAbstractShape()
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

    public ParentShape getTriangle()
    {
        return triangle;
    }

    public Ricin getRicin()
    {
        return ricin;
    }

}

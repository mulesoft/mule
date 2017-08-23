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
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.List;
import java.util.Map;

@Extension(name = "SubtypesConnector")
@Operations(SubTypesTestOperations.class)
@Sources(SubtypesSource.class)
@ConnectionProviders(SubTypesConnectionProvider.class)
@Import(type = Ricin.class)
@Import(type = Weapon.class)
@Import(type = VeganCookBook.class)
@SubTypeMapping(baseType = ParentShape.class, subTypes = {Square.class, Triangle.class})
@SubTypeMapping(baseType = Door.class, subTypes = {HouseDoor.class, CarDoor.class})
@SubTypeMapping(baseType = Weapon.class, subTypes = {Revolver.class})
@Export(classes = {Revolver.class})
@Xml(prefix = "subtypes", namespace = "http://www.mulesoft.org/schema/mule/subtypes")
public class SubTypesMappingConnector {

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
  private List<PojoForList> onePojos;

  @Parameter
  private List<PojoForList> twoPojos;

  @Parameter
  private List<Weapon> weapons;

  @Parameter
  private Map<String, Weapon> weaponMap;

  public Map<String, Weapon> getWeaponMap() {
    return weaponMap;
  }

  public List<Weapon> getWeapons() {
    return weapons;
  }

  public void setWeapons(List<Weapon> weapons) {
    this.weapons = weapons;
  }

  public List<PojoForList> getOnePojos() {
    return onePojos;
  }

  public List<PojoForList> getTwoPojos() {
    return twoPojos;
  }

  public ExtensiblePojo getPojoWithExtension() {
    return pojoWithExtension;
  }

  public Weapon getExtensibleWeapon() {
    return extensibleWeapon;
  }

  public ParentShape getAbstractShape() {
    return abstractShape;
  }

  public Door getDoorInterface() {
    return doorInterface;
  }

  public Square getExplicitSquare() {
    return explicitSquare;
  }

  public FinalPojo getFinalPojo() {
    return finalPojo;
  }

  public ParentShape getTriangle() {
    return triangle;
  }

  public Ricin getRicin() {
    return ricin;
  }

}

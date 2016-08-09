/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.module.extension.internal.config.AbstractConfigParserTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.subtypes.extension.CarDoor;
import org.mule.test.subtypes.extension.FinalPojo;
import org.mule.test.subtypes.extension.HouseDoor;
import org.mule.test.subtypes.extension.ParentShape;
import org.mule.test.subtypes.extension.PojoForList;
import org.mule.test.subtypes.extension.Revolver;
import org.mule.test.subtypes.extension.Square;
import org.mule.test.subtypes.extension.SubTypesConnectorConnection;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.subtypes.extension.Triangle;
import org.mule.test.vegan.extension.VeganCookBook;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.List;

import org.junit.Test;

public class SubTypesMappingParserTestCase extends AbstractConfigParserTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {VeganExtension.class, HeisenbergExtension.class, SubTypesMappingConnector.class};
  }

  @Override
  protected String getConfigFile() {
    return "subtypes-mapping.xml";
  }

  @Test
  public void shapeRetriever() throws Exception {
    MuleEvent responseEvent = flowRunner("shapeRetriever").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), instanceOf(ParentShape.class));

    ParentShape payload = (ParentShape) responseEvent.getMessage().getPayload();
    assertThat(payload.getArea(), is(16));
  }

  @Test
  public void doorRetriever() throws Exception {
    MuleEvent responseEvent = flowRunner("doorRetriever").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), instanceOf(CarDoor.class));

    CarDoor payload = (CarDoor) responseEvent.getMessage().getPayload();
    assertThat(payload.getColor(), is("blue"));
  }

  @Test
  public void configRetriever() throws Exception {
    MuleEvent responseEvent = flowRunner("configRetriever").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), instanceOf(SubTypesMappingConnector.class));

    SubTypesMappingConnector payload = (SubTypesMappingConnector) responseEvent.getMessage().getPayload();
    assertThat(payload.getAbstractShape(), instanceOf(Square.class));
    assertThat(payload.getAbstractShape().getArea(), is(1));

    assertThat(payload.getExplicitSquare(), instanceOf(Square.class));
    assertThat(payload.getExplicitSquare().getArea(), is(4));

    assertThat(payload.getDoorInterface(), instanceOf(CarDoor.class));
    assertThat(payload.getFinalPojo(), instanceOf(FinalPojo.class));

    assertThat(payload.getRicin(), instanceOf(Ricin.class));
    assertThat(payload.getTriangle(), instanceOf(Triangle.class));

    assertThat(payload.getPojoListOne(), is(notNullValue()));
    assertThat(payload.getPojoListOne(), hasSize(1));
    assertThat(payload.getPojoListOne().get(0), instanceOf(PojoForList.class));
    assertThat(payload.getPojoListOne().get(0).getId(), is("inner"));

    assertThat(payload.getWeaponList(), is(notNullValue()));
    assertThat(payload.getWeaponList(), hasSize(4));

    assertRevolver(payload.getWeaponList().get(0), 6);
    assertRicin(payload.getWeaponList().get(1), 10L, "Krazy-8");
    assertRicin(payload.getWeaponList().get(2), 20L, "Lidia");
    assertRevolver(payload.getWeaponList().get(3), 0);

    assertThat(payload.getWeaponMap(), is(notNullValue()));
    assertThat(payload.getWeaponMap().entrySet(), hasSize(4));
    assertRicin(payload.getWeaponMap().get("ricinChild"), 20L, "Lidia");
    assertRevolver(payload.getWeaponMap().get("revolverChild"), 1);
    assertRevolver(payload.getWeaponMap().get("revolverRef"), 6);
    assertRevolver(payload.getWeaponMap().get("revolverMEL"), 0);

  }

  @Test
  public void connectionRetriever() throws Exception {
    MuleEvent responseEvent = flowRunner("connectionRetriever").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), instanceOf(SubTypesConnectorConnection.class));

    SubTypesConnectorConnection payload = (SubTypesConnectorConnection) responseEvent.getMessage().getPayload();
    assertThat(payload.getDoor(), instanceOf(HouseDoor.class));
    assertThat(payload.getShape(), instanceOf(Triangle.class));
    assertThat(payload.getShape().getArea(), is(1));
  }

  @Test
  public void subtypedAndConcreteParameters() throws Exception {
    MuleEvent responseEvent = flowRunner("subtypedAndConcreteParameters").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), notNullValue());

    List<Object> payload = (List<Object>) responseEvent.getMessage().getPayload();
    assertThat(payload, hasSize(6));

    assertThat(payload.get(0), instanceOf(ParentShape.class));
    assertThat(((ParentShape) payload.get(0)).getArea(), is(2));

    assertThat(payload.get(1), instanceOf(HouseDoor.class));
    assertThat(((HouseDoor) payload.get(1)).isLocked(), is(false));

    assertThat(payload.get(2), instanceOf(FinalPojo.class));
    assertThat(((FinalPojo) payload.get(2)).getSomeString(), is("asChild"));

    assertThat(payload.get(3), instanceOf(VeganCookBook.class));
    assertThat(((VeganCookBook) payload.get(3)).getNumberOfPages(), is(1));

    assertThat(payload.get(4), instanceOf(Square.class));
    assertThat(((Square) payload.get(4)).getSide(), is(4));
    assertThat(((Square) payload.get(4)).getArea(), is(16));

    assertThat(payload.get(5), instanceOf(Triangle.class));
    assertThat(((Triangle) payload.get(5)).getHeight(), is(4));
    assertThat(((Triangle) payload.get(5)).getArea(), is(2));
  }

  @Test
  public void subtypedAndConcreteParametersAsAttributes() throws Exception {
    MuleEvent responseEvent = flowRunner("subtypedAndConcreteParametersAsAttributes").withPayload("").run();

    assertThat(responseEvent.getMessage().getPayload(), notNullValue());

    List<Object> payload = (List<Object>) responseEvent.getMessage().getPayload();
    assertThat(payload, hasSize(6));

    assertThat(payload.get(0), instanceOf(ParentShape.class));
    assertThat(((ParentShape) payload.get(0)).getArea(), is(9));

    assertThat(payload.get(1), instanceOf(CarDoor.class));
    assertThat(((CarDoor) payload.get(1)).getColor(), is("white"));

    assertThat(payload.get(2), instanceOf(FinalPojo.class));
    assertThat(((FinalPojo) payload.get(2)).getSomeString(), is("globalString"));

    assertThat(payload.get(3), instanceOf(VeganCookBook.class));
    assertThat(((VeganCookBook) payload.get(3)).getNumberOfPages(), is(10));

    assertThat(payload.get(4), instanceOf(Square.class));
    assertThat(((Square) payload.get(4)).getSide(), is(3));
    assertThat(((Square) payload.get(4)).getArea(), is(9));

    assertThat(payload.get(5), instanceOf(Triangle.class));
    assertThat(((Triangle) payload.get(5)).getHeight(), is(6));
    assertThat(((Triangle) payload.get(5)).getArea(), is(3));

  }

  @Test
  public void subtypeContributionToOtherExtension() throws Exception {
    HeisenbergExtension heisenberg = lookupHeisenberg("heisenberg");
    assertThat(heisenberg, is(notNullValue()));

    List<? extends Weapon> wildCardWeapons = heisenberg.getWildCardWeapons();
    assertThat(wildCardWeapons, is(notNullValue()));
    assertThat(wildCardWeapons, hasSize(2));

    assertThat(wildCardWeapons.get(0), instanceOf(Ricin.class));
    assertThat(((Ricin) wildCardWeapons.get(0)).getMicrogramsPerKilo(), is(10L));
    assertThat(((Ricin) wildCardWeapons.get(0)).getDestination(), is(notNullValue()));
    assertThat(((Ricin) wildCardWeapons.get(0)).getDestination().getVictim(), is("Krazy-8"));

    assertThat(wildCardWeapons.get(1), instanceOf(Revolver.class));
    assertThat(((Revolver) wildCardWeapons.get(1)).getBullets(), is(3));
  }

  @Test
  public void mappedParamSource() throws Exception {
    try {
      ((Flow) getFlowConstruct("sourceWithParameterMapping")).start();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void assertRicin(Object payload, Long micrograms, String victim) {
    assertThat(payload, instanceOf(Ricin.class));
    assertThat(((Ricin) payload).getMicrogramsPerKilo(), is(micrograms));
    assertThat(((Ricin) payload).getDestination(), is(notNullValue()));
    assertThat(((Ricin) payload).getDestination().getVictim(), is(victim));
  }

  private void assertRevolver(Object payload, int bullets) {
    assertThat(payload, instanceOf(Revolver.class));
    assertThat(((Revolver) payload).getBullets(), is(bullets));
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Base class for tests using heisenberg-config.xml configuration.
 */
public abstract class AbstractHeisenbergConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    final Map<String, Object> objects = new HashMap<>();

    objects.put("recipes", ImmutableMap.<String, Long>builder()
        .put("methylamine", 75l)
        .put("pseudoephedrine", 0l)
        .put("P2P", 25l)
        .build());

    objects.put("deathsBySeasons", ImmutableMap.<String, List<String>>builder()
        .put("s01", ImmutableList.<String>builder()
            .add("emilio")
            .add("domingo")
            .build())
        .put("s02", ImmutableList.<String>builder()
            .add("tuco")
            .add("tortuga")
            .build())
        .build());

    Ricin ricin = new Ricin(new KnockeableDoor("Lidia", "Stevia coffe shop"), 22L);
    objects.put("labeledRicins", ImmutableMap.<String, Ricin>builder()
        .put("pojo", ricin)
        .build());

    objects.put("monthlyIncomes", ImmutableList.<Long>builder()
        .add(12000L)
        .add(500L)
        .build());

    objects.put("enemies", ImmutableList.<String>builder()
        .add("Gustavo Fring")
        .add("Hank")
        .build());

    objects.put("ricinPacks", ImmutableSet.<Ricin>builder()
        .add(ricin).build());


    objects.put("candidateDoors", ImmutableMap.<String, KnockeableDoor>builder()
        .put("skyler", new KnockeableDoor("Skyler", "308 Negra Arroyo Lane"))
        .put("saul", new KnockeableDoor("Saul", "Shopping Mall"))
        .build());

    return objects;
  }
}

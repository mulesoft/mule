/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_PACKS_SUMMARY;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;

import java.util.Map;
import java.util.Set;

public class RicinGroup {

  @Parameter
  @Optional
  @Placement(order = 1)
  private Map<String, Ricin> labeledRicin;

  @Parameter
  @Optional
  private KnockeableDoor nextDoor;

  @Parameter
  @Optional
  @Placement(order = 2)
  @Summary(RICIN_PACKS_SUMMARY)
  private Set<Ricin> ricinPacks;

  public Map<String, Ricin> getLabeledRicin() {
    return labeledRicin;
  }

  public KnockeableDoor getNextDoor() {
    return nextDoor;
  }

  public Set<Ricin> getRicinPacks() {
    return ricinPacks;
  }
}

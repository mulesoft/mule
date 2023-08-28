/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_PACKS_SUMMARY;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.sdk.api.annotation.param.display.Placement;
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

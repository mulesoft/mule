package org.mule.tooling.extensions.metadata.api.parameters;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ItemListOutput {

  private List<ItemOutput> itemOutputs;

  public ItemListOutput(ItemOutput... items) {
    this.itemOutputs = Collections.unmodifiableList(Arrays.asList(items));
  }

  public List<ItemOutput> getItemOutputs() {
    return itemOutputs;
  }
}

package org.mule.tooling.extensions.metadata.api.parameters;

public class ItemOutput {

  private String id;
  private String name;

  public ItemOutput(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}

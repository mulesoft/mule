/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.internal.routing.split.AbstractMessageSequence;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @since 3.6.0
 */
public class NodeListMessageSequence extends AbstractMessageSequence<Node> {

  private final NodeList nodeList;
  private int index = 0;

  public NodeListMessageSequence(NodeList nodeList) {
    this.nodeList = nodeList;
  }

  @Override
  public Integer size() {
    return nodeList.getLength();
  }

  @Override
  public boolean hasNext() {
    return index < size();
  }

  @Override
  public Node next() {
    return nodeList.item(index++);
  }
}

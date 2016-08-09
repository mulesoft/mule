/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.xml.el;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.module.xml.xpath.XPathReturnType;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPath3TestCase extends FunctionalTestCase {

  private static final String HANDKERCHIEF = "handkerchief";
  private static final String FOR_THE_SAME_HANDKERCHIEF = "For the same handkerchief?";
  private static final Double LINES_COUNT = 3556D;

  @Override
  protected String getConfigFile() {
    return "xpath3-config.xml";
  }

  @Test
  public void findLinesWithParameterAsString() throws Exception {
    Object lines = findLines(HANDKERCHIEF, XPathReturnType.STRING);
    assertThat(lines, instanceOf(String.class));
    assertThat((String) lines, equalTo(FOR_THE_SAME_HANDKERCHIEF));
  }

  @Test
  public void findLinesWithParameterAsNode() throws Exception {
    Object lines = findLines(HANDKERCHIEF, XPathReturnType.NODE);
    assertThat(lines, instanceOf(Node.class));
    assertThat(((Node) lines).getTextContent(), equalTo(FOR_THE_SAME_HANDKERCHIEF));
  }

  @Test
  public void findLinesWithParameterAsNodeList() throws Exception {
    Object lines = findLines(HANDKERCHIEF, XPathReturnType.NODESET);
    assertThat(lines, instanceOf(NodeList.class));
    NodeList list = (NodeList) lines;
    assertThat(list.getLength(), is(27));
    assertThat(list.item(0).getTextContent(), equalTo(FOR_THE_SAME_HANDKERCHIEF));
  }

  @Test
  public void findLinesWithParameterAsBoolean() throws Exception {
    Object lines = findLines(HANDKERCHIEF, XPathReturnType.BOOLEAN);
    assertThat(lines, instanceOf(Boolean.class));
    assertThat((Boolean) lines, is(true));
  }

  @Test
  public void findLinesWithParameterAsNumber() throws Exception {
    Object lines = findLines(HANDKERCHIEF, XPathReturnType.NUMBER);
    assertThat(lines, instanceOf(Double.class));
    assertThat((Double) lines, is(Double.NaN));
  }

  @Test
  public void concatOverResultNode() throws Exception {
    Node line = (Node) findLines(HANDKERCHIEF, XPathReturnType.NODE);
    String result = getPayloadAsString(flowRunner("actTitles").withPayload(line).run().getMessage());
    assertThat(result, equalTo("ACT III SCENE III.  The garden of the castle."));
  }

  @Test
  public void getAncestorOverResultNode() throws Exception {
    Node line = (Node) findLines(HANDKERCHIEF, XPathReturnType.NODE);
    String result = getPayloadAsString(flowRunner("findSpeaker").withPayload(line).run().getMessage());
    assertThat(result, equalTo("EMILIA"));
  }

  @Test
  public void countLines() throws Exception {
    Object result = flowRunner("countLines").withPayload(getOthello()).run().getMessage().getPayload();
    assertThat(result, instanceOf(Double.class));
    assertThat((Double) result, equalTo(LINES_COUNT));
  }

  @Test
  public void payloadConsumed() throws Exception {
    MuleEvent event = flowRunner("payloadConsumed").withPayload(getOthello()).run();
    assertThat((String) event.getFlowVariable("result"), equalTo("3556"));
    assertThat(event.getMessage().getPayload(), instanceOf(Node.class));
  }

  @Test
  public void foreach() throws Exception {
    List<Node> nodes = new ArrayList<>();
    flowRunner("foreach").withPayload(getOthello()).withFlowVariable("nodes", nodes).run();

    assertThat(nodes.size(), is(LINES_COUNT.intValue()));
  }

  private Object findLines(String word, XPathReturnType type) throws Exception {
    return flowRunner("shakespeareLines").withPayload(getOthello()).withFlowVariable("word", word)
        .withFlowVariable("returnType", type.name()).run().getMessage().getPayload();
  }

  private InputStream getOthello() throws IOException {
    return IOUtils.getResourceAsStream("othello.xml", getClass());
  }
}

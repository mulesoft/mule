/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.email;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_CONTENT;
import static org.mule.extension.email.util.EmailTestUtils.EMAIL_SUBJECT;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.mule.extension.email.util.EmailTestUtils.setUpServer;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class EmailConnectorTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public DynamicPort PORT = new DynamicPort("port");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  protected GreenMail server;
  protected GreenMailUser user;

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    ServerSetup serverSetup = setUpServer(PORT.getNumber(), getProtocol());
    server = new GreenMail(serverSetup);
    server.start();
    user = server.setUser(JUANI_EMAIL, JUANI_EMAIL, "password");
  }

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    assertThat(server, is(not(nullValue())));
    server.stop();
  }

  protected void assertBodyContent(String content) {
    assertThat(content, is(EMAIL_CONTENT));
  }

  protected void assertSubject(String content) {
    assertThat(content, is(EMAIL_SUBJECT));
  }

  public abstract String getProtocol();
}

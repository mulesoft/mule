/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.sftp;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;
import static com.jcraft.jsch.ChannelSftp.SSH_FX_PERMISSION_DENIED;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.mule.extension.ftp.internal.sftp.connection.SftpClient;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.tck.size.SmallTest;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.SftpException;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SftpClientTestCase {

  private static final String FILE_PATH = "/bla/file.txt";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Path path;

  @Mock
  private JSch jsch;

  @Mock
  private ChannelSftp channel;

  @InjectMocks
  private SftpClient client = new SftpClient(EMPTY, 0, () -> jsch);;

  @Before
  public void setUp() {
    when(path.toString()).thenReturn(FILE_PATH);
  }

  @Test
  public void returnNullOnUnexistingFile() throws Exception {
    when(channel.stat(any())).thenThrow(new SftpException(SSH_FX_NO_SUCH_FILE, "No such file"));
    assertThat(client.getAttributes(path), is(nullValue()));
  }

  @Test
  public void exceptionIsThrownOnError() throws Exception {
    expectedException.expect(MuleRuntimeException.class);
    expectedException.expectMessage(format("Could not obtain attributes for path %s", FILE_PATH));
    when(channel.stat(any())).thenThrow(new SftpException(SSH_FX_PERMISSION_DENIED, EMPTY));
    client.getAttributes(path);
  }
}

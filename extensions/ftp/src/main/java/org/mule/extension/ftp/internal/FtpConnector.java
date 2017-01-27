/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.internal;

import org.mule.extension.file.common.api.FileConnectorConfig;
import org.mule.extension.file.common.api.FilePredicateBuilder;
import org.mule.extension.file.common.api.exceptions.FileError;
import org.mule.extension.ftp.api.FtpFileAttributes;
import org.mule.extension.ftp.api.FtpFilePredicateBuilder;
import org.mule.extension.ftp.api.ftp.ClassicFtpFileAttributes;
import org.mule.extension.ftp.api.sftp.SftpFileAttributes;
import org.mule.extension.ftp.internal.ftp.connection.ClassicFtpConnectionProvider;
import org.mule.extension.ftp.internal.sftp.connection.SftpConnectionProvider;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;

import javax.inject.Inject;

/**
 * Allows manipulating files through the FTP and SFTP
 *
 * @since 4.0
 */
@Extension(name = "FTP", description = "Connector to manipulate Files on a FTP/SFTP server")
@Operations({FtpOperations.class})
@SubTypeMapping(baseType = FilePredicateBuilder.class, subTypes = FtpFilePredicateBuilder.class)
@ConnectionProviders({ClassicFtpConnectionProvider.class, SftpConnectionProvider.class})
@Export(classes = {SftpFileAttributes.class, ClassicFtpFileAttributes.class, FtpFileAttributes.class})
@ErrorTypes(FileError.class)
@Xml(namespace = "ftp")
public class FtpConnector extends FileConnectorConfig {

  public static final String FTP_PROTOCOL = "ftp";

  @Inject
  private ConnectionManager connectionManager;


  public ConnectionManager getConnectionManager() {
    return connectionManager;
  }
}

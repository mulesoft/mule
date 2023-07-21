/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.connection.config;

public class ClassloaderConfigConnection {

  private String fileContent;

  public ClassloaderConfigConnection(String fileContent){
    this.fileContent = fileContent;
  }

  public String getFileContent(){
    return fileContent;
  }

  public void invalidate(){}
}
/*
 */
package org.mule.providers.vfs.transformers;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: Jun 28, 2005
 * Time: 11:30:18 AM
 */
public class TextMessage {
  private String message;

  public TextMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }
}

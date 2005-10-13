package org.mule.providers.vfs.transformers;

import org.apache.commons.vfs.FileObject;
import org.mule.config.i18n.Message;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.InputStream;

/**
 * Get the content from <code>org.apache.commons.vfs.FileObject</code> instance as a <code>String</code>
 * User: Ian de Beer
 * Date: Jun 9, 2005
 * Time: 8:48:33 AM
 */
public class FileToTextMessage extends AbstractTransformer {

   public FileToTextMessage() {
        registerSourceType(FileObject.class);
        setReturnClass(TextMessage.class);
    }

  public Object doTransform(Object msg) throws TransformerException {
    String result = null;
    if (msg instanceof FileObject) {
      try {
        InputStream in = ((FileObject)msg).getContent().getInputStream();
        byte[] content = new byte[(int)((FileObject)msg).getContent().getSize()];
        in.read(content);
        result = new String(content);
      }
      catch (Exception e) {
        throw new TransformerException(this,e);
      }
    }
    else {
      throw new TransformerException(Message.createStaticMessage("Message is not an instance of org.apache.commons.vfs.FileObject"),this);
    }
    return new TextMessage(result);
  }
}

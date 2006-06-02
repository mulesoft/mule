package org.mule.providers.vfs;

import org.mule.providers.AbstractMessageAdapter;
import org.apache.commons.vfs.FileObject;

import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 16, 2005
 * Time: 11:13:19 PM
 */
public class VFSMessageAdapter  extends AbstractMessageAdapter {
  private FileObject[] files;
  private FileObject file;
  private Object payload;

  public VFSMessageAdapter(FileObject[] files) {
    if (files.length == 1) {
      this.file = files[0];
      payload = this.file;
    }
    else {
      this.files = files;
      payload = this.files;
    }
  }

  public VFSMessageAdapter(FileObject file) {
    this.file = file;
    payload = this.file;
  }

  public String getPayloadAsString() throws Exception {
    StringBuffer stringBuffer = new StringBuffer();
    byte[] buffer =null;
    if (payload instanceof FileObject[] ) {
      for (int i = 0; i < files.length; i++) {
        InputStream inputStream = files[i].getContent().getInputStream();
        buffer = new byte[inputStream.available()];
        inputStream.read(buffer);
        inputStream.close();
        stringBuffer.append(files[i].getName().getPath());
        stringBuffer.append(": \n");
        stringBuffer.append(new String(buffer));
      }
    }
    else {
      InputStream inputStream = file.getContent().getInputStream();
      buffer = new byte[inputStream.available()];
      inputStream.read(buffer);
      stringBuffer.append(new String(buffer));
      inputStream.close();
    }
    return stringBuffer.toString();
  }

  public byte[] getPayloadAsBytes() throws Exception {
    byte[] buffer =null;
    if (payload instanceof FileObject[] ) {
      int bufferLength = 0;
      for (int i = 0; i < files.length; i++) {
        InputStream inputStream = files[i].getContent().getInputStream();
        bufferLength += inputStream.available();
        inputStream.close();
      }
      buffer = new byte[bufferLength];
      int start = 0;
      for (int i = 0; i < files.length; i++) {
        InputStream inputStream = files[i].getContent().getInputStream();
        int length = inputStream.available();
        inputStream.read(buffer,start,length);
        inputStream.close();
        start += length;
      }
    }
    else {
      InputStream inputStream = file.getContent().getInputStream();
      buffer = new byte[inputStream.available()];
      inputStream.read(buffer);
      inputStream.close();
    }
    return buffer;
  }

  public Object getPayload() {
    return payload;
  }
}

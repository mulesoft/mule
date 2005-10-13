package org.mule.providers.vfs;

import org.apache.commons.vfs.FileFilter;
import org.apache.commons.vfs.FileFilterSelector;
import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.providers.file.FileConnector;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 28, 2005
 * Time: 11:13:47 AM
 */
public class VFSMessageDispatcher extends AbstractMessageDispatcher {

  private VFSConnector connector;


  public VFSMessageDispatcher(VFSConnector connector) {
    super(connector);
    this.connector = connector;
  }


  public void doDispose() {
  }

  public void doDispatch(UMOEvent event) throws Exception {
    try {
      Object data = event.getTransformedMessage();
      String filename = (String) event.getProperty(FileConnector.PROPERTY_FILENAME);

      if (filename == null) {
        String outPattern = (String) event.getProperty(FileConnector.PROPERTY_OUTPUT_PATTERN);
        if (outPattern == null) {
          outPattern = connector.getOutputPattern();
        }
        filename = generateFilename(event, outPattern);
      }

      if (filename == null) {
        throw new IOException("Filename is null");
      }

      FileObject file = connector.getFsManager().resolveFile(connector.getDirectory() + "/" + filename);
      byte[] buf;
      if (data instanceof byte[]) {
        buf = (byte[]) data;
      }
      else {
        buf = data.toString().getBytes();
      }

      logger.info("Writing file to: " + file.getURL());
      OutputStream outputStream = file.getContent().getOutputStream(connector.isOutputAppend());
      try {
        outputStream.write(buf);
      }
      finally {
        outputStream.close();
      }
      if (connector.isVersionControlled()) {
        if (!connector.isOutputAppend()) {
          connector.getWCClient().doAdd(new File(file.getName().getPath()), true, true,true,true);
        }
        File[] paths = {new File(file.getName().getPath())};
        SVNCommitClient commitClient = connector.getCommitClient();
        SVNCommitPacket packet = commitClient.doCollectCommitItems(paths,true,true,true);

        commitClient.doCommit(packet,true,"Praxis");
      }
    }
    catch (Exception e) {
      getConnector().handleException(e);
    }
  }

  public UMOMessage doSend(UMOEvent event) throws Exception {
    doDispatch(event);
    return event.getMessage();
  }

  public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
    UMOMessage result = null;
    if (connector.getSelectExpression() != null && (!connector.getSelectExpression().equals(""))) {
      FileObject[] files = connector.getDirObject().findFiles(new FileFilterSelector(new FileFilter(){
        public boolean accept(FileSelectInfo fileInfo) {
          return fileInfo.getFile().getName().getPath().matches(connector.getSelectExpression());
        }
      }));
      new MuleMessage(connector.getMessageAdapter(files));
    }
    return result;

  }

  public Object getDelegateSession() throws UMOException {
    return null;
  }

  public UMOConnector getConnector() {
    return connector;
  }

  private String generateFilename(UMOEvent event, String pattern) {
    if (pattern == null) {
      pattern = connector.getOutputPattern();
    }
    return connector.getFilenameParser().getFilename(event.getMessage(), pattern);
  }


}

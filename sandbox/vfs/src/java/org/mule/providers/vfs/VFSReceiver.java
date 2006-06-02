package org.mule.providers.vfs;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSelectInfo;
import org.apache.commons.vfs.FileSelector;
import org.apache.commons.vfs.FileType;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.quartz.JobDetail;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.util.zip.CheckedInputStream;
import java.util.zip.Adler32;
import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 21, 2005
 * Time: 1:42:30 PM
 */
public class VFSReceiver extends ScheduledMessageReceiver {

  private static Map checksumMap = Collections.synchronizedMap(new HashMap());
  private FileObject dirObject;
  private boolean versionControlled;
  private String fileExtension;
  private boolean includeSubfolders;
  private SVNUpdateClient updateClient;


  public VFSReceiver(org.mule.providers.vfs.VFSConnector connector, UMOComponent component,
                     UMOEndpoint endpoint)
        throws InitialisationException {
    super(connector, component, endpoint, connector.getTrigger());
    dirObject = connector.getDirObject();
    versionControlled = connector.isVersionControlled();
    updateClient = connector.getUpdateClient();
    fileExtension = connector.getFileExtension();
    includeSubfolders = connector.isIncludeSubFolders();
  }

  public void execute(JobDetail jobDetail) {
    try {
      // update from svn to get modified files
      if (versionControlled) {
        updateClient.doUpdate(new File(dirObject.getName().getPath()),SVNRevision.HEAD,true);
      }

      if (dirObject.exists()) {
        if (dirObject.getType() == FileType.FOLDER) {
          FileObject[] files = null;
          files = dirObject.findFiles(new FileSelector() {
            public boolean includeFile(FileSelectInfo fileInfo) throws java.lang.Exception {
              if ((!fileInfo.getFile().isHidden()) && (fileInfo.getFile().getType() != FileType.FOLDER)) {
                if (fileExtension.equals("*"))  {
                  return true;
                }
                else {
                  return (fileInfo.getFile().getName().getPath().endsWith(fileExtension));
                }
              }
              else {
                return false;
              }
            }
            public boolean traverseDescendents(FileSelectInfo fileInfo) throws java.lang.Exception {
              if (includeSubfolders) {
                return (!fileInfo.getFile().isHidden());
              }
              else {
                return ((!fileInfo.getFile().isHidden()) && fileInfo.getDepth() < 2);
              }
            }
          });
          for (int i = 0; i < files.length; i++) {
            if (hasChanged(files[i])) {
              processFile(files[i]);
            }
          }
        }
      }
    }
    catch (Exception e) {
      connector.handleException(e);
    }
  }

  private void processFile(FileObject fileObject) {
    try {
      UMOMessageAdapter msgAdapter = connector.getMessageAdapter(fileObject);
      msgAdapter.setProperty(VFSConnector.PROPERTY_ORIGINAL_FILENAME, fileObject.getName().getPath());
      UMOMessage message = new MuleMessage(msgAdapter);
      routeMessage(message, endpoint.isSynchronous());
    }
    catch (Throwable e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public void doConnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void doDisconnect() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  protected boolean hasChanged(FileObject fileObject) {
    boolean changed = false;
    String key = fileObject.getName().getPath();
    long checksum = 0;
    if (checksumMap.containsKey(key)) {
      checksum = ((Long) checksumMap.get(key)).longValue();
    }
    long newChecksum = 0;
    CheckedInputStream checkedInputStream = null;
    try {
      InputStream inputStream = fileObject.getContent().getInputStream();
      checkedInputStream = new CheckedInputStream(inputStream, new Adler32());
      byte[] buffer = new byte[inputStream.available()];
      while (checkedInputStream.read(buffer) >= 0) {
        ;
      }
      newChecksum = checkedInputStream.getChecksum().getValue();
      if (newChecksum != checksum) {
        if (logger.isDebugEnabled()) {
          logger.debug("calculated a new checksum of " + newChecksum);
        }
        checksumMap.put(key, new Long(newChecksum));
        changed = true;
      }
    }
    catch (IOException e) {
      connector.handleException(e);
    }
    return changed;
  }
}

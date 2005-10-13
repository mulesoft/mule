package org.mule.providers.vfs;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.FileType;
import org.apache.commons.vfs.VFS;
import org.apache.commons.vfs.provider.local.LocalFile;
import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.providers.file.FilenameParser;
import org.mule.providers.file.SimpleFilenameParser;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;
import org.quartz.Trigger;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 27, 2005
 * Time: 12:05:48 AM
 */
public class VFSConnector extends AbstractServiceEnabledConnector {

  public static final String PROPERTY_DIRECTORY = "directory";
  public static final String PROPERTY_OUTPUT_PATTERN = "outputPattern";
  public static final String PROPERTY_VERSIONCONTROLLED = "versionControlled";
  public static final String PROPERTY_FILENAME = "filename";
  public static final String PROPERTY_ORIGINAL_FILENAME = "originalFilename";
  public static final String PROPERTY_SELECT_EXPRESSION = "selectExpression";
  public static final String PROPERTY_FILE_EXTENSION = "fileExtension";
  public static final String PROPERTY_INCLUDE_SUBFOLDERS = "includeSubfolders";

//  private SVNUpdateClient updateClient;
//  private SVNUpdateClient commitClient;
//  private SVNUpdateClient wcClient;


  private String outputPattern;
  private FilenameParser filenameParser;
  private Trigger trigger;
  private String directory;
  private FileSystemManager fsManager;
  private FileObject dirObject;
  private String selectExpression;
  private boolean includeSubFolders = false;
  private boolean outputAppend = false;
  private boolean versionControlled = false;
  private String fileExtension = "*";
  private ISVNOptions options;
  private ISVNAuthenticationManager authManager;
  private SVNUpdateClient updateClient;
  private SVNCommitClient commitClient;
  private SVNWCClient wcClient;


  public VFSConnector() {
    filenameParser = new SimpleFilenameParser();
  }

  public String getProtocol() {
    return "VFS";
  }

  public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception {
    directory = endpoint.getEndpointURI().getAddress();
    fsManager = VFS.getManager();
    dirObject = fsManager.resolveFile(directory);
    Map props = endpoint.getProperties();

    //Override properties on the endpoint for the specific endpoint

    if (props != null) {
      String overrideFileExtension = (String) props.get(PROPERTY_FILE_EXTENSION);
      if (overrideFileExtension != null) {
        fileExtension = overrideFileExtension;
      }
      String overrideIncludeSubFolders = (String) props.get(PROPERTY_INCLUDE_SUBFOLDERS);
      if (overrideIncludeSubFolders != null) {
        includeSubFolders = new Boolean(overrideIncludeSubFolders).booleanValue();
      }
    }

    if (dirObject.getType() == FileType.FOLDER) {
      // if versionControlled was set to true determine whether the directory is under version control
      if (dirObject instanceof LocalFile) {
        if (versionControlled) {
          versionControlled = SVNWCUtil.isVersionedDirectory(new File(directory));
        }
      }
      else {
        // version control can only be applied to local files
        versionControlled = false;
      }
      UMOMessageReceiver fileReceiver = new VFSReceiver(this, component, endpoint);
      if (versionControlled) {
        options = SVNWCUtil.createDefaultOptions(true);
        authManager = SVNWCUtil.createDefaultAuthenticationManager();
      }
      return fileReceiver;
    }
    else {
      throw new MuleException(Message.createStaticMessage("Path is not a directory"));
    }
  }

  public boolean isIncludeSubFolders() {
    return includeSubFolders;
  }

  public void setIncludeSubFolders(boolean includeSubFolders) {
    this.includeSubFolders = includeSubFolders;
  }

  public Trigger getTrigger() {
    return trigger;
  }

  public void setTrigger(Trigger trigger) {
    this.trigger = trigger;
  }

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public String getOutputPattern() {
    return outputPattern;
  }

  public void setOutputPattern(String outputPattern) {
    this.outputPattern = outputPattern;
  }

  public boolean isOutputAppend() {
    return outputAppend;
  }

  public void setOutputAppend(boolean outputAppend) {
    this.outputAppend = outputAppend;
  }

  public FilenameParser getFilenameParser() {
    return filenameParser;
  }

  public void setFilenameParser(FilenameParser filenameParser) {
    this.filenameParser = filenameParser;
  }

  public boolean isVersionControlled() {
    return versionControlled;
  }

  public void setVersionControlled(boolean versionControlled) {
    this.versionControlled = versionControlled;
  }

  public SVNUpdateClient getUpdateClient() {
    if (updateClient == null) {
      updateClient =new SVNUpdateClient(authManager, options);
    }
    return updateClient;
  }

  public SVNCommitClient getCommitClient() {
    if (commitClient == null) {
      commitClient = new SVNCommitClient(authManager, options);
    }
    return commitClient;
  }

  public SVNWCClient getWCClient() {
    if (wcClient == null) {
      wcClient = new SVNWCClient(authManager, options);
    }
    return wcClient;
  }

  public FileSystemManager getFsManager() {
    return fsManager;
  }

  public void setFsManager(FileSystemManager fsManager) {
    this.fsManager = fsManager;
  }

  public FileObject getDirObject() {
    return dirObject;
  }

  public void setDirObject(FileObject dirObject) {
    this.dirObject = dirObject;
  }

  public String getSelectExpression() {
    return selectExpression;
  }

  public void setSelectExpression(String selectExpression) {
    this.selectExpression = selectExpression;
  }

  public String getFileExtension() {
    return fileExtension;
  }

  public void setFileExtension(String fileExtension) {
    this.fileExtension = fileExtension;
  }
}

package sailpoint.services.standard.task;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.log4j.Logger;

import sailpoint.api.ObjectUtil;
import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.Link;
import sailpoint.object.QueryOptions;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * Import manual correlations from a CSV file. This can be a file exported using
 * the Manual Correlation Exporter task or created manually in the correct
 * format. Each row must have application name, account name and identity name
 * in that order, with a header on the first row. The account name can be either
 * the 'Identity Attribute' or 'Display Attribute' defined in the application
 * schema.
 *
 * @author <a href="mailto:paul.wheeler@sailpoint.com">Paul Wheeler</a>
 */

public class ManualCorrelationImporter extends AbstractTaskExecutor {

   private static final Logger log = Logger
         .getLogger(ManualCorrelationImporter.class);

   public static final String IMPORT_FILE = "importFile";

   private boolean terminate = false;

   private int totalImported = 0;
   private int alreadyCorrelated = 0;
   private int accountsNotFound = 0;
   private int identitiesNotFound = 0;

   @Override
   public void execute(SailPointContext context, TaskSchedule taskSchedule,
         TaskResult taskResult, Attributes<String, Object> attributes)
         throws GeneralException, IOException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

      log.debug("Starting Manual Correlation Importer");

      String importFile = attributes.getString(IMPORT_FILE);

      if (log.isDebugEnabled())
         log.debug("Import file: " + importFile);

      BufferedReader br = null;
      try {
         br = new BufferedReader(new FileReader(importFile));
         String line = br.readLine(); // First line is the header
         if (line != null) {
            line = br.readLine();
         }
         int counter = 0;
         while (line != null) {
            if (!line.equals("")) {
               List<String> attrs = Util.csvToList(line);
               String appName = attrs.get(0);
               String acctName = attrs.get(1);
               String identityName = attrs.get(2);
               updateProgress(context, taskResult, "Correlating account "
                     + acctName + " on application " + appName
                     + " to identity " + identityName);

               if (null != acctName && acctName.equals("???")) {
                  // Ignore the special '???' name that IdentityIQ sets when we
                  // didn't provide a nativeIdentity when the account was
                  // provisioned.
                  acctName = null;
               }
               if (log.isDebugEnabled())
                  log.debug("Processing account: " + acctName
                        + " on application " + appName);

               Application app = context.getObjectByName(Application.class,
                     appName);
               if (null == app) {
                  throw new GeneralException("Application '" + appName
                        + "' could not be found.");
               }

               // build a query based on application and account name
               QueryOptions ops = new QueryOptions();

               ops.addFilter(Filter.or(
                     Filter.ignoreCase(Filter.eq("displayName", acctName)),
                     Filter.ignoreCase(Filter.eq("nativeIdentity", acctName))));
               ops.addFilter(Filter.eq("application", app));
               List<Link> links = context.getObjects(Link.class, ops);

               if (links != null && !links.isEmpty()) {
                  Link link = links.get(0);
                  if (link.getIdentity() != null) {
                     if (log.isDebugEnabled())
                        log.debug("Found link on identity "
                              + link.getIdentity().getName());
                  }
                  // Get the identity that currently has the link
                  Identity sourceIdentity = link.getIdentity();

                  // Get the identity that we are moving the link to
                  Identity targetIdentity = context.getObjectByName(Identity.class,
                        identityName);
                  if (targetIdentity == null) {
                     if (log.isDebugEnabled())
                        log.debug("Could not find identity " + identityName);
                     identitiesNotFound++;
                  } else if (sourceIdentity != null) {
                     if (sourceIdentity == targetIdentity) {
                        log.debug("Account " + acctName + " on application "
                              + appName + " is already correlated to identity "
                              + identityName);
                        alreadyCorrelated++;
                     } else {
                        // Move the link but attempt to acquire a lock on both
                        // of the affected identities first.
                        String sourceIdentityName = sourceIdentity.getName();

                        Identity lockedSourceIdentity = acquireIdentityLock(
                              context, sourceIdentityName,
                              "Manual Correlation Import", 10, 3);
                        if (null == lockedSourceIdentity) {
                           throw new GeneralException(
                                 "Unable to acquire lock on identity "
                                       + identityName);
                        } else {
                           Identity lockedTargetIdentity = acquireIdentityLock(
                                 context, identityName,
                                 "Manual Correlation Import", 10, 3);
                           if (null == lockedTargetIdentity) {
                              ObjectUtil.unlockIdentity(context,
                                    lockedSourceIdentity);
                              throw new GeneralException(
                                    "Unable to acquire lock on identity "
                                          + identityName);
                           } else {
                              sourceIdentity.remove(link);
                              context.saveObject(sourceIdentity);
                              targetIdentity.add(link);
                              context.saveObject(targetIdentity);
                              link.setManuallyCorrelated(true);
                              context.commitTransaction();

                              ObjectUtil.unlockIdentity(context,
                                    lockedSourceIdentity);
                              ObjectUtil.unlockIdentity(context,
                                    lockedTargetIdentity);
                              totalImported++;
                              if (log.isDebugEnabled())
                                 log.debug("Moved account " + acctName
                                       + " on application " + appName
                                       + " from identity "
                                       + sourceIdentity.getName()
                                       + " to identity "
                                       + targetIdentity.getName());
                           }
                        }

                     }
                  }
               } else {
                  if (log.isDebugEnabled())
                     log.debug("Account " + acctName
                           + " not found for application " + appName);
                  accountsNotFound++;
               }
            }
            counter++;
            if (counter > 19) {
               // Decache every 20 records to avoid hibernate cache bloat
               context.decache();
               counter = 0;
            }
            line = br.readLine();
         }

      } finally {
         taskResult.setAttribute("totalImported", totalImported);
         taskResult.setAttribute("alreadyCorrelated", alreadyCorrelated);
         taskResult.setAttribute("accountsNotFound", accountsNotFound);
         taskResult.setAttribute("identitiesNotFound", identitiesNotFound);
         if (br != null)
           br.close();
         log.debug("Exiting Manual Correlation Importer");
      }

   }

   /**
    * A helper function that attempts to acquire a lock on an Identity. It will
    * wait for 'waitSecs' for any existing locks to go away before giving up an
    * attempt, and it will re-attempt 'retryTimes' before giving up entirely. On
    * a successful lock it will return a valid sailpoint.object.Identity
    * reference. If it fails to acquire a lock then it will return a null
    * Identity reference to the caller and it will display various messages in
    * the log file. After the last re-attempt it will give up and log a full
    * stack trace allowing system administrators to review the issue. The
    * 'lockName' argument is an option string that can describe the process that
    * is acquiring the lock on the Identity. If null or an empty string is
    * passed for this then the host + thread name will be substituted in for the
    * value of 'lockName'.
    * 
    * @throws UnknownHostException
    * @throws GeneralException
    * @throws SecurityException 
    * @throws NoSuchMethodException 
    * @throws InvocationTargetException 
    * @throws IllegalArgumentException 
    * @throws IllegalAccessException 
    */
   public Identity acquireIdentityLock(SailPointContext context,
         String identityId, String lockName, int waitSecs, int retryTimes)
         throws UnknownHostException, GeneralException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

      // Sanity check the arguments passed in, to prevent irrationally short
      // calls.
      if (retryTimes <= 0)
         retryTimes = 1;
      if (waitSecs <= 0)
         waitSecs = 5;

      // Make sure we've been asked to lock a valid Identity.
      Identity idToLock = context.getObjectById(Identity.class, identityId);
      if (null == idToLock) {
         idToLock = context.getObjectByName(Identity.class, identityId);
      }
      if (null == idToLock) {
         log.error("Could not find an Identity to lock matching: ["
               + identityId + "]");
         return null;
      }

      int numLockRetries = 0;
      Identity lockedId = null;

      // If no lock name was passed in then create one that's descriptive of
      // the host and thread that acquired the lock came from.
      if ((lockName instanceof String) && (0 == lockName.length())) {
         lockName = null;
      }
      if (null == lockName) {
         String hostName = java.net.InetAddress.getLocalHost().getHostName();
         Long threadId = Thread.currentThread().getId();
         String threadName = Thread.currentThread().getName();
         lockName = "host:[" + hostName + "], thread ID:[" + threadId
               + "], thread:[" + threadName + "]";
      }

      // ObjectUtil.lockIdentity takes different arguments in 7.3+
      // so use Reflection to manage this.
      Method lockIdentityOld = null;
  	  Method lockIdentityNew = null;
  	  
  	  try {
  	     lockIdentityNew = ObjectUtil.class.getMethod("lockIdentity", SailPointContext.class, String.class, int.class); 
      }  catch (NoSuchMethodException e) {
	     // lockIdentityNew will be null if the method doesn't exist.
	  }
  	  
  	  if (lockIdentityNew == null) {
  	     lockIdentityOld = ObjectUtil.class.getMethod("lockIdentity", SailPointContext.class, String.class, String.class, int.class); 
  	  }
  	  
  	  Object[] params = null;
  	  if (lockIdentityNew == null) {
  		 params = new Object[] { context, identityId, lockName, waitSecs };
  	  } else {
  		 params = new Object[] { context, identityId, waitSecs };
  	  }
  	  
      while ((lockedId == null) && (numLockRetries < retryTimes)) {

         try {

        	// Attempt to acquire a lock in the object.
        	if (lockIdentityNew == null) {
        	   lockedId = (Identity) lockIdentityOld.invoke(null, params);
        	} else {
        	   lockedId = (Identity) lockIdentityNew.invoke(null, params);
        	}
        	
         } catch (InvocationTargetException ex) {
        	 
        	if (ex.getCause() instanceof sailpoint.api.ObjectAlreadyLockedException) {
	            // Let's see who's got this object currently locked.
	            String lockString = idToLock.getLock();
	            if ((null == lockString) || (0 == lockString.length())) {
	               lockString = "unspecified";
	            }
	
	            // Log the stack trace on the final attempt to retry.
	            if (numLockRetries == (retryTimes - 1)) {
	               String eMsg = "Failed to acquire lock on Identity ["
	                     + identityId + "], lock held by: [" + lockString + "]";
	               log.error(eMsg, ex);
	            } else {
	               String wMsg = "Timeout acquiring lock on Identity ["
	                     + identityId + "], lock held by: [" + lockString
	                     + "], retrying.";
	               log.warn(wMsg);
	            }
        	} else {
        		throw new InvocationTargetException(ex);
        	}

         }
         numLockRetries++;
      }
      return lockedId;
   }

   @Override
   public boolean terminate() {
      terminate = true;
      return terminate;
   }

}

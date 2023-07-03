package sailpoint.services.standard.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sailpoint.api.IncrementalObjectIterator;
import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Certification;
import sailpoint.object.CertificationAction;
import sailpoint.object.CertificationDefinition;
import sailpoint.object.CertificationGroup;
import sailpoint.object.Filter;
import sailpoint.object.QueryOptions;
import sailpoint.object.TaskResult;
import sailpoint.object.TaskSchedule;
import sailpoint.task.AbstractTaskExecutor;
import sailpoint.tools.Util;

/**
 * Close active certifications that have reached their end date
 *
 * @author <a href="mailto:paul.wheeler@sailpoint.com">Paul Wheeler</a>
 */

public class CertificationCloser extends AbstractTaskExecutor {
   private static Log log = LogFactory.getLog(CertificationCloser.class);

   private static final String CERTIFICATION_GROUPS = "certGroups";

   boolean terminate = false;

   /**
    * Main task execution method
    */
   public void execute(SailPointContext context, TaskSchedule schedule,
         TaskResult result, Attributes<String, Object> args) throws Exception {

      if (log.isDebugEnabled())
         log.debug("Starting Certification Closer");

      updateProgress(context, result, "Starting task");

      int certificationsClosed = 0;
      Date now = new Date();
      String certGroups = args.getString(CERTIFICATION_GROUPS);
      if (log.isDebugEnabled()) log.debug("CertificationGroups: " + certGroups);
      
      List<String> certGroupsProcessed = new ArrayList<String>();

      // CertificationGroups are passed in as a csv of IDs.
      // Put them in a list.
      List<String> certGroupList = new ArrayList<String>();
      if (null != certGroups) {
         certGroupList = Util.csvToList(certGroups);
      }

      QueryOptions qo = new QueryOptions();
      qo.addFilter(Filter.eq("phase", Certification.Phase.End));
      qo.addFilter(Filter.ne("immutable",true));
      IncrementalObjectIterator<Certification> it = new IncrementalObjectIterator(context, Certification.class, qo);
      while ((null != it) && (it.hasNext()) && !terminate) {
         Certification certification = (Certification) it.next();
         List<CertificationGroup> certificationGroups = certification
               .getCertificationGroups();
         if (null != certificationGroups && !certificationGroups.isEmpty()) {
            boolean hasActiveCertGroup = false;
            String certificationGroupId = null;
            String certificationGroupName = null;
            for (CertificationGroup certificationGroup : certificationGroups) {
               certificationGroupId = certificationGroup.getId();
               certificationGroupName = certificationGroup.getName();
               if (certificationGroup.getStatus().equals(
                     CertificationGroup.Status.Active)) {
                  hasActiveCertGroup = true;
                  context.decache(certificationGroup);
                  break;
               }
            }
            if (hasActiveCertGroup) {
               // Ignore the CertificationGroup if it's not in the list and list
               // is not empty.
               if (certGroupList.isEmpty()
                     || certGroupList.contains(certificationGroupId) 
            	   		 || certGroupList.contains(certificationGroupName)) {
            	   
                  CertificationDefinition certificationDefinition = certification
                        .getCertificationDefinition(context);
                  if (null != certificationDefinition) {
                     if (log.isDebugEnabled())
                        log.debug("Processing Certification "
                              + certification.getName()
                              + " with CertificationDefinition "
                              + certificationDefinition);
                     updateProgress(context, result, "Processing "
                           + certification.getName());
                     boolean certDefChanged = false;
                     if (!certificationDefinition.isAutomaticClosingEnabled()) {
                        certificationDefinition
                              .setAutomaticClosingEnabled(true);
                        certificationDefinition
                              .setAutomaticClosingComments("Closed by the Certification Closer task");
                        certificationDefinition
                              .setAutomaticClosingSignerName("spadmin");
                        certDefChanged = true;
                     }

                     // Do a separate check of the closing action, just in case
                     // there are any that were previously set up to reject
                     CertificationAction.Status closingAction = certificationDefinition
                           .getAutomaticClosingAction();
                     if (!CertificationAction.Status.Approved
                           .equals(closingAction)) {
                        certificationDefinition
                              .setAutomaticClosingAction(CertificationAction.Status.Approved);
                        certDefChanged = true;
                     }
                     if (certDefChanged) {
                        context.saveObject(certificationDefinition);
                        context.commitTransaction();
                     }
                     context.decache(certificationDefinition);

                     // Force the certification to get processed by Perform
                     // Maintenance
                     certification.setAutomaticClosingDate(now);
                     certification.setNextPhaseTransition(now);
                     context.saveObject(certification);
                     context.commitTransaction();
                     // Keep track of how many CertificationGroups have been
                     // affected
                     if (!certGroupsProcessed.contains(certificationGroupId)) {
                        certGroupsProcessed.add(certificationGroupId);
                     }
                     certificationsClosed++;
                  }
               }
            }
         }
         context.decache(certification);
      }

      result.setAttribute("certificationsClosed", certificationsClosed);
      result.setAttribute("certGroupsProcessed", certGroupsProcessed.size());
      if (log.isDebugEnabled())
         log.debug("Exiting Certification Closer");
   }

   public boolean terminate() {

      terminate = true;

      return terminate;
   }

}

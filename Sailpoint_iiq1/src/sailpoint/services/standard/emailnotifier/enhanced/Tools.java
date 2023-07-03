package sailpoint.services.standard.emailnotifier.enhanced;

import sailpoint.api.EmailNotifier;
import sailpoint.object.Configuration;
import sailpoint.server.RedirectingEmailNotifier;
import sailpoint.server.SMTPEmailNotifier;
import sailpoint.tools.GeneralException;

public class Tools {

	
	/**
	 * Derive the email notifier class to send the content to.
	 */
	public static final EmailNotifier getEmailNotifierClass() throws GeneralException {
		
		Configuration config = Configuration.getSystemConfig();
		EmailNotifier notifier = null;
		
		// Grab the email notifier type
        String type = config.getString(Configuration.EMAIL_NOTIFIER_TYPE);

        // This is required.  We should have upgraded this already.
        if (null == type) {
            throw new GeneralException(Configuration.EMAIL_NOTIFIER_TYPE + " not configured in system config.");
        }

        if (Configuration.EMAIL_NOTIFIER_TYPE_SMTP.equals(type)) {
            notifier = new SMTPEmailNotifier();
        }
        else if (Configuration.EMAIL_NOTIFIER_TYPE_REDIRECT_TO_EMAIL.equals(type) || 
                Configuration.EMAIL_NOTIFIER_TYPE_REDIRECT_TO_FILE.equals(type)) {
        	
            RedirectingEmailNotifier redirecting = new RedirectingEmailNotifier();
            redirecting.setDelegate(new SMTPEmailNotifier());
            redirecting.setEmailAddress(config.getString(Configuration.REDIRECTING_EMAIL_NOTIFIER_ADDRESS));
            if (Configuration.EMAIL_NOTIFIER_TYPE_REDIRECT_TO_FILE.equals(type)) {
                redirecting.setFileName(config.getString(Configuration.REDIRECTING_EMAIL_NOTIFIER_FILENAME));
            } else {
                redirecting.setFileName(null);
            }
            notifier = redirecting;
        }
        else {
            throw new GeneralException("Unknown email notifier type: " + type);
        }
        
        return notifier;
        
	}
	
}

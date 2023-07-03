package sailpoint.services.standard.emailnotifier.enhanced;

import sailpoint.api.EmailNotifier;
import sailpoint.api.SailPointContext;
import sailpoint.object.EmailOptions;
import sailpoint.object.EmailTemplate;
import sailpoint.tools.EmailException;
import sailpoint.tools.GeneralException;

public class RedirectToFolderNotifier implements EmailNotifier {

	private boolean sendImmediate = false;
	@Override
	public void sendEmailNotification(SailPointContext context, EmailTemplate template, EmailOptions options) throws GeneralException, EmailException {
		
		
		
	}

	@Override
	public Boolean sendImmediate() {
		return sendImmediate;
	}

	@Override
	public void setSendImmediate(Boolean sendImmediate) {
		this.sendImmediate = sendImmediate;
		
	}

}

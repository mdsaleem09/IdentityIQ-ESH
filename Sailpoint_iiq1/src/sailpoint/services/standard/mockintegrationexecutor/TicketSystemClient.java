package sailpoint.services.standard.mockintegrationexecutor;

import java.util.HashMap;

import org.apache.log4j.Logger;

import sailpoint.object.Attributes;
import sailpoint.object.Custom;
import sailpoint.object.ProvisioningResult;
import sailpoint.tools.GeneralException;

public class TicketSystemClient {

    private static final Logger logger = Logger.getLogger(TicketSystemClient.class);
    static final String TICKETS = "tickets";
    static final String CUSTOM_OBJECT_NAME = "customObjectName";
    static final String SIMULATED_LATENCY = "simulatedLatency";

    MockIntegrationExecutor executor;

    /** A sailpoint custom object that holds the ticket numbers and status */
    Custom custom = new Custom();

    public TicketSystemClient(MockIntegrationExecutor executor) throws GeneralException {
        this.executor = executor;
        logger.debug("Start MockIntegration");
        logger.debug("Thread Name:" + Thread.currentThread().getName() + ", id:" + Thread.currentThread().getId());
    }

    /**
     * Add a ticket to IIQ by adding an entry to the "tickets" map in the custom
     * object
     * 
     * @param requestId
     * @return
     * @throws GeneralException
     */
    ProvisioningResult provision(String requestId) throws GeneralException {
        ProvisioningResult provisioningResult = new ProvisioningResult();
        retrieveCustomObject();
        Attributes<String, Object> attrs = custom.getAttributes();
        @SuppressWarnings("unchecked")
        HashMap<String, String> tickets = (HashMap<String, String>) attrs.get(TICKETS);
        tickets.put(requestId, ProvisioningResult.STATUS_QUEUED);
        attrs.put(TICKETS, tickets);
        custom.setAttributes(attrs);

        executor.getContext().saveObject(custom);
        executor.getContext().commitTransaction();
        provisioningResult.setStatus(ProvisioningResult.STATUS_QUEUED);
        provisioningResult.setRequestID(requestId);
        return provisioningResult;
    }

    ProvisioningResult checkStatus(String requestId) throws GeneralException {
        ProvisioningResult provisioningResult = new ProvisioningResult();
        retrieveCustomObject();
        Attributes<String, Object> attrs = custom.getAttributes();
        @SuppressWarnings("unchecked")
        HashMap<String, String> tickets = (HashMap<String, String>) attrs.get(TICKETS);
        String currentStatus = tickets.get(requestId);// put(identityRequestNumber,
                                                      // ProvisioningResult.STATUS_QUEUED);
        if (null == currentStatus) {
            provisioningResult.setStatus(ProvisioningResult.STATUS_FAILED);
            provisioningResult.addError("The ticket was not found");
        } else {
            provisioningResult.setStatus(currentStatus);
        }
        return provisioningResult;
    }

    /** Get the custom object from IIQ, if it does not yet exist create one. */
    void retrieveCustomObject() throws GeneralException {
        logger.debug("retrieveCustomObject()");
        logger.debug("customObjectName: " + executor.getCustomObjectName());
        custom = executor.context.getObjectByName(Custom.class, executor.getCustomObjectName());
        if (null == custom)
            createNewCustomObject();
    }

    /** Create a custom object and persist it to IIQ */
    void createNewCustomObject() throws GeneralException {
        logger.debug("createNewCustomObject()");
        custom = new Custom();
        Attributes<String, Object> attributes = new Attributes<String, Object>();

        // The tickets
        HashMap<String, String> tickets = new HashMap<String, String>();
        attributes.put(TICKETS, tickets);

        custom.setAttributes(attributes);
        custom.setName(executor.getCustomObjectName());
        logger.debug("save custom object with name:" + executor.getCustomObjectName());
        executor.context.saveObject(custom);
        executor.context.commitTransaction();
    }

}

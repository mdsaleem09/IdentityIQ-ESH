package sailpoint.services.standard.mockintegrationexecutor;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.integration.AbstractIntegrationExecutor;
import sailpoint.object.Attributes;
import sailpoint.object.IntegrationConfig;
import sailpoint.object.ProvisioningPlan;
import sailpoint.object.ProvisioningResult;
import sailpoint.tools.GeneralException;

public class MockIntegrationExecutor extends AbstractIntegrationExecutor {

    private static final Logger logger = Logger.getLogger(MockIntegrationExecutor.class);

    SailPointContext context;
    private TicketSystemClient ticketSystemClient;
    IntegrationConfig config;
    private boolean assumeSuccessfulTicketOperations = false;
    
    static final String ASSUME_SUCCESSFUL_TICKET_OPERATIONS = "assumeSuccessfulTicketOperations";

    /** The name of the sailpoint.object.cutom that holds the tickets. */
    String customObjectName = null;

    /** The amount of time to wait to simulate latency in the ticket create APIs */
    private int simulatedLatency = 100;

    public MockIntegrationExecutor() throws GeneralException {
        logger.debug("Start MockIntegrationExecutor");
        ticketSystemClient = new TicketSystemClient(this);
    }

    public void configure(SailPointContext context, IntegrationConfig config) throws Exception {
        logger.debug("configure start()");
        super.configure(context, config);
        this.setCustomObjectName((String) config.getAttribute(TicketSystemClient.CUSTOM_OBJECT_NAME));
        this.setAssumeSuccessfulTicketOperations(Boolean.valueOf((String) config.getAttribute(MockIntegrationExecutor.ASSUME_SUCCESSFUL_TICKET_OPERATIONS)));
        this.simulatedLatency = config.getInt(TicketSystemClient.SIMULATED_LATENCY);
        this.context = context;
        this.config = config;
    }

    public void setCustomObjectName(String customObjectName) {
        this.customObjectName = customObjectName;
    }

    public String getCustomObjectName() {
        return this.customObjectName;
    }

    @Override
    public ProvisioningResult checkStatus(String requestId) throws Exception {
        logger.debug("Inside checkStatus:" + requestId);
        ProvisioningResult provisioningResult = new ProvisioningResult();
        logger.debug("simulated latency sleep start (" + simulatedLatency + " ms)");
        Thread.sleep(simulatedLatency);
        logger.debug("simulated latency sleep end");

        if(assumeSuccessfulTicketOperations) {
            provisioningResult.setStatus(ProvisioningResult.STATUS_COMMITTED);
            provisioningResult.setRequestID(Long.toString(System.currentTimeMillis()) ) ;
            logger.debug("assumeSuccessfulTicketOperations in checkStatus()");
            return provisioningResult;
        }
        
        
        if (context != null) {
            if (null == requestId || requestId.isEmpty()) {
                provisioningResult.addError("The requestId was null or empty");
                provisioningResult.setStatus(ProvisioningResult.STATUS_FAILED);
            } else {
                provisioningResult = ticketSystemClient.checkStatus(requestId);
            }
        } else {
            provisioningResult.setStatus(ProvisioningResult.STATUS_FAILED);
            provisioningResult.addError("The context was null");
        }
        return provisioningResult;
    }

    @Override
    public String ping() throws Exception {
        return "Success";
    }

    @Override
    public ProvisioningResult provision(ProvisioningPlan plan) throws Exception {
        logger.debug("Start provision(). Thread Name:" + Thread.currentThread().getName() + ", id:"
                + Thread.currentThread().getId());
        ProvisioningResult provisioningResult = new ProvisioningResult();
        
        logger.debug("simulated latency sleep start (" + simulatedLatency + " ms)");
        Thread.sleep(simulatedLatency);
        logger.debug("simulated latency sleep end");

        if(assumeSuccessfulTicketOperations) {
            provisioningResult.setStatus(ProvisioningResult.STATUS_QUEUED);
            provisioningResult.setRequestID(Long.toString(System.currentTimeMillis()) ) ;
            logger.debug("assumeSuccessfulTicketOperations = true");
            return provisioningResult;
        } else {
            logger.debug("assumeSuccessfulTicketOperations = false");
        }
        
        String identityRequestNo = null;
        if (null != plan && null != context) {
            Attributes<String, Object> planArgs = plan.getArguments();
            if (planArgs != null) {
                identityRequestNo = (String) planArgs.get("identityRequestId");
                logger.debug("identityRequestNo:" + identityRequestNo);
            }
            if (null == identityRequestNo || identityRequestNo.isEmpty()) {
                provisioningResult.addError("The identityRequestNo was null or empty");
                provisioningResult.setStatus(ProvisioningResult.STATUS_FAILED);
            } else {
                provisioningResult = ticketSystemClient.provision(identityRequestNo);
            }
        } else {
            provisioningResult.setStatus(ProvisioningResult.STATUS_FAILED);
            provisioningResult.addError("The context or plan was null");
        }
        return provisioningResult;
    }

    boolean isAssumeSuccessfulTicketOperations() {
        return assumeSuccessfulTicketOperations;
    }

    void setAssumeSuccessfulTicketOperations(boolean assumeSuccessfulTicketOperations) {
        this.assumeSuccessfulTicketOperations = assumeSuccessfulTicketOperations;
    }


}

import org.apache.ofbiz.entity.GenericEntityException;
def createOfbizDemo() {
    result = [:];
    try {
        atparTechie  = delegator.makeValue("atparTechie");
        // Auto generating next sequence of atparTechieId primary key
        atparTechie.setNextSeqId();
        // Setting up all non primary key field values from context map
        atparTechie.setNonPKFields(context);
        // Creating record in database for atparTechie entity for prepared value
        atparTechie = delegator.create(atparTechie);
        result.atparTechieId = atparTechie.atparTechieId;
        logInfo("==========This is my first Groovy Service implementation in Apache OFBiz. atparTechie record "
                + "created successfully with atparTechieId: "+ atparTechie.getString("atparTechieId"));
    } catch (GenericEntityException e) {
        logError(e.getMessage());
        return error("Error in creating record in atparTechie entity ........");
    }
    return result;
}


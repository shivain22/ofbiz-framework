
import org.apache.ofbiz.base.util.UtilDateTime
import org.apache.ofbiz.base.util.UtilProperties
import org.apache.ofbiz.base.util.UtilValidate
import org.apache.ofbiz.entity.condition.EntityConditionBuilder
import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.entity.serialize.XmlSerializer

import org.apache.ofbiz.service.ServiceUtil

def checkAtparProductRelatedPermission(String callingMethodName, String checkAction) {
    if (!callingMethodName) {
        callingMethodName = UtilProperties.getMessage("CommonUiLabels", "CommonPermissionThisOperation", parameters.locale)
    }
    if (UtilValidate.isEmpty(checkAction)) {
        checkAction = "UPDATE"
    }
    List roleCategories = []
    // find all role-categories that this product is a member of
    if (parameters.atparProductId && !security.hasEntityPermission("ATPAR_PRODUCT", "_${checkAction}", parameters.userLogin)) {
        Map lookupRoleCategoriesMap = [atparProductId : parameters.atparProductId,
                                       partyId        : userLogin.partyId,
                                       roleTypeId     : "LTD_ADMIN"]
        roleCategories = from("AtparProductCategoryMemberAndRole")
                .where(lookupRoleCategoriesMap)
                .filterByDate("roleFromDate", "roleThruDate")
                .queryList()
    }

    if (!(security.hasEntityPermission("ATPAR_PRODUCT", "_${checkAction}", parameters.userLogin)
            || (roleCategories && security.hasEntityPermission("ATPAR_PRODUCT_ROLE", "_${checkAction}", parameters.userLogin))
            || (parameters.alternatePermissionRoot &&
            security.hasEntityPermission(parameters.alternatePermissionRoot, "_${checkAction}", parameters.userLogin)))) {
        String checkActionLabel = "AtparProductCatalog${checkAction.charAt(0)}${checkAction.substring(1).toLowerCase()}PermissionError"
        return error(UtilProperties.getMessage("AtparProductUiLabels", checkActionLabel,
                [resourceDescription: callingMethodName, mainAction: checkAction], parameters.locale))
    }
    return success()
}


def updateAtparProductStatus() {
    // Check for the necessary permissions
//    Map res = checkAtparProductRelatedPermission("updateAtparProductStatus", "UPDATE")
//    if (!ServiceUtil.isSuccess(res)) {
//        return res
//    }

    // Fetch the atparProduct record based on given parameters
    if (parameters.productId!=null){
        GenericValue lookedUpValue = from("atparProduct").where("productId",parameters.productId).queryOne()
        if (lookedUpValue == null) {
            return ServiceUtil.returnError("AtparProduct not found for the given parameters.")
        }
        if(parameters.status==0){
            lookedUpValue.set("status", 1)
        }
        else if(parameters.status==1){
            lookedUpValue.set("status", 2)
        }


        // Store the updated record
        lookedUpValue.store()

        // Return success message
        return ServiceUtil.returnSuccess("Product Content updated successfully.")
    }

    else{
        GenericValue lookedUpValue = from("atparProduct").where("atparProductId",parameters.atparProductId).queryOne()
        if (lookedUpValue == null) {
            return ServiceUtil.returnError("AtparProduct not found for the given parameters.")
        }
        if(parameters.status==0){
            lookedUpValue.set("status", 1)
        }
        else if(parameters.status==1){
            lookedUpValue.set("status", 2)
        }


        // Store the updated record
        lookedUpValue.store()

        // Return success message
        return ServiceUtil.returnSuccess("Product Approved successfully.")
    }

    return ServiceUtil.returnError("Product Approved Failed")

    // Update the status field to 1

}

def findAtparProductStatusFalse(){
    Map result = success()

    Map performFindParams =parameters
    performFindParams.inputFields=parameters.inputFields
    performFindParams.entityName=parameters.entityName
    performFindParams.orderBy=parameters.orderBy
    performFindParams.viewIndex=parameters.viewIndex
    performFindParams.viewSize=parameters.viewSize
    performFindParams.inputFields.status = 0L

    Map  atparProductStatusFalse = run service: "performFind", with: performFindParams
    return atparProductStatusFalse

}

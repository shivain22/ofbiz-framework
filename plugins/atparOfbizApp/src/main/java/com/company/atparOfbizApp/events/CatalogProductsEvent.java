package com.company.atparOfbizApp.events;

import org.apache.ofbiz.base.util.*;
import org.apache.ofbiz.entity.Delegator;
import org.apache.ofbiz.entity.GenericEntityException;
import org.apache.ofbiz.entity.GenericValue;
import org.apache.ofbiz.entity.condition.EntityCondition;
import org.apache.ofbiz.entity.condition.EntityConditionList;
import org.apache.ofbiz.entity.model.DynamicViewEntity;
import org.apache.ofbiz.entity.util.EntityListIterator;
import org.apache.ofbiz.entity.util.EntityQuery;
import org.apache.ofbiz.entity.util.EntityUtilProperties;
import org.apache.ofbiz.service.DispatchContext;
import org.apache.ofbiz.service.GenericServiceException;
import org.apache.ofbiz.service.LocalDispatcher;
import org.apache.ofbiz.service.ServiceUtil;

import java.sql.Timestamp;
import java.util.*;

import static org.apache.ofbiz.base.util.UtilGenerics.checkCollection;
import static org.apache.ofbiz.base.util.UtilGenerics.checkMap;

public class CatalogProductsEvent {
    private static final String RESOURCE = "CommonUiLabels";
    private static final String PRODUCTCATALOGCATEGORY = "ProdCatalogCategory";
    private static final String CATALOG = "ProdCatalog";
    private static final String NOCONDITIONFIND = "Y";
    private static final String MODULE = CatalogProductsEvent.class.getName();

    public static Map<String, Object> getCatalogProducts(DispatchContext dctx, Map<String, ?> context) throws GenericServiceException {

        Map<String, Object> result = new HashMap<>();
        Delegator delegator = dctx.getDelegator();

        String noConditionFind =NOCONDITIONFIND;
        Locale locale = (Locale) context.get("locale");
        String prodCatalogId= (String) context.get("prodCatalogId");
        LocalDispatcher dispatcher = dctx.getDispatcher();
        Integer viewIndex = (Integer) context.get("viewIndex");
        if(viewIndex==null){
            viewIndex=0;
        }
        Map inputFields =new HashMap();
        if(prodCatalogId!=null) {
            inputFields.put("prodCatalogId", prodCatalogId);
        }


        Map<String, Object> executeResult = null;
        try {
            executeResult = dispatcher.runSync("performFind", UtilMisc.toMap("entityName", PRODUCTCATALOGCATEGORY,"inputFields",inputFields,
                    "noConditionFind", noConditionFind,"viewIndex", viewIndex));

        } catch (GenericServiceException gse) {
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "CommonFindErrorRetrieveIterator",
                    UtilMisc.toMap("errorString", gse.getMessage()), locale));
        }

        EntityListIterator listIt= (EntityListIterator) executeResult.get("listIt");
        List<Object> resultList = new ArrayList<>();
        List<GenericValue> catalogProducts= new ArrayList<>();
        if (listIt != null) {
            GenericValue entity;
            while ((entity = listIt.next()) != null) {
                String productCategoryId= entity.getString("productCategoryId");
                if(!resultList.contains(productCategoryId)){
                    catalogProducts= getProductCategoryMembers(dispatcher,catalogProducts,productCategoryId);

                    resultList.add(productCategoryId);
                }
            }
        }


        GenericValue catalog;
        try {
            catalog = EntityQuery.use(delegator).from(CATALOG).where("prodCatalogId", prodCatalogId).cache().queryOne();
        } catch (GenericEntityException e) {
            Debug.logWarning(e.getMessage(), MODULE);
            catalog = null;
        }

        if(catalog!=null){result.put("prodCatalog", catalog);}
        result.put("catalogProducts", catalogProducts);
        return result;
    }

    public static List<GenericValue> getProductCategoryMembers(LocalDispatcher dispatcher,  List<GenericValue> catalogProducts,String productCategoryId) throws GenericServiceException {
        Map<String, Object> productCategoryAndLimitedMembers= dispatcher.runSync("getProductCategoryAndLimitedMembers",UtilMisc.toMap("productCategoryId", productCategoryId));
        catalogProducts.addAll((Collection<? extends GenericValue>) productCategoryAndLimitedMembers.get("productCategoryMembers"));
        return catalogProducts;
    }

}

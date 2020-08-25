package com.helphalf.quickbook.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.ipp.data.Account;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.exception.FMSException;
import com.intuit.ipp.exception.InvalidTokenException;
import com.intuit.ipp.services.DataService;
import com.intuit.ipp.services.QueryResult;
import com.intuit.ipp.util.Config;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
public class SettingController {

    @Autowired
    OAuth2PlatformClientFactory factory;


    private static final Logger logger = Logger.getLogger(SettingController.class);


    @ResponseBody
    @GetMapping("/setting")
    public String callAccountInfo() {

        String realmId = ContextFactory.companyID;

        if (StringUtils.isEmpty(realmId)) {
            return new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
        }
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;

        String failureMsg = "Failed";
//        System.out.println("accesstoken----"+accessToken);
//        System.out.println("refreshToken----"+refreshToken);
//        System.out.println("realmId----"+realmId);
        DataServiceFactory serviceClient = new DataServiceFactory();

        String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";
//        String result = "";
        try {

            // set custom config
            Config.setProperty(Config.BASE_URL_QBO, url);

            //get DataService
            DataService service = serviceClient.getDataService(realmId, accessToken);

            // get all accout
//            select * from Account where AccountType in  ('Income','Other Current Asset')
            String sqlIncome = "select * from account where AccountType = 'Income'";
            QueryResult queryResultAccount = service.executeQuery(sqlIncome);
            String income_accounts = JSON.toJSONString(queryResultAccount);

            String sqlAsset = "select * from account where AccountType = 'Other Current Asset'";
            QueryResult queryResultAsset = service.executeQuery(sqlAsset);
            String asset_accounts = JSON.toJSONString(queryResultAsset);

            String sqlTaxAgencies = "select * from TaxAgency";
            QueryResult queryResultTax = service.executeQuery(sqlTaxAgencies);
            String tax_agencies = JSON.toJSONString(queryResultTax);
//            result = JSON.toJSONString(queryResult);

            Map<String, Object> incomeMap = JSON.parseObject(income_accounts, Map.class);
            Map<String, Object> assetMap = JSON.parseObject(asset_accounts, Map.class);
            Map<String, Object> taxMap = JSON.parseObject(tax_agencies, Map.class);

//            Map<String, Object> income = (Map<String, Object>) incomeMap.get("entities");
//            Map<String, Object> asset = (Map<String, Object>) assetMap.get("entities");

            Map<String, Object> mapSetting = new HashMap<String, Object>();
            mapSetting.put("income_accounts", incomeMap.get("entities"));
            mapSetting.put("asset_accounts", assetMap.get("entities"));
            mapSetting.put("tax_agencies", taxMap.get("entities"));

            return mapSetting.toString();
//            return processResponse(failureMsg, queryResult);
//            return result;
        }
        catch (InvalidTokenException e) {
            logger.error("Error while calling executeQuery :: " + e.getMessage());

            //refresh tokens
            logger.info("received 401 during account call, refreshing tokens now");
            OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
//            String refreshToken = (String)session.getAttribute("refresh_token");

            try {
                BearerTokenResponse bearerTokenResponse = client.refreshToken(accessToken);
//                session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
//                session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
                ContextFactory.bearerToken = bearerTokenResponse.getAccessToken();
                ContextFactory.refreshToken = bearerTokenResponse.getRefreshToken();

                //call company info again using new tokens
                logger.info("calling account using new tokens");
                DataService service = serviceClient.getDataService(realmId, accessToken);

                // get all companyinfo
                String sql = "select * from account";
                QueryResult queryResult = service.executeQuery(sql);
                return processResponse(failureMsg, queryResult);

            } catch (OAuthException e1) {
                logger.error("Error while calling bearer token :: " + e.getMessage());
                return new JSONObject().put("response",failureMsg).toString();
            } catch (FMSException e1) {
                logger.error("Error while calling account currency :: " + e.getMessage());
                return new JSONObject().put("response",failureMsg).toString();
            }

        } catch (FMSException e) {
            List<Error> list = e.getErrorList();
            list.forEach(error -> logger.error("Error while calling executeQuery :: " + error.getMessage()));
            return new JSONObject().put("response",failureMsg).toString();
        }
    }

    private String processResponse(String failureMsg, QueryResult queryResult) {
        if (!queryResult.getEntities().isEmpty() && queryResult.getEntities().size() > 0) {
            Account account = (Account) queryResult.getEntities().get(0);
            logger.info("account -> account: " + account.getBankNum());
            ObjectMapper mapper = new ObjectMapper();
            try {
                String jsonInString = mapper.writeValueAsString(account);
                return jsonInString;
            } catch (JsonProcessingException e) {
                logger.error("Exception while getting account info ", e);
                return new JSONObject().put("response",failureMsg).toString();
            }

        }
        return failureMsg;
    }

//    private DataService getDataService(String realmId, String accessToken) throws FMSException {
//
//        //create oauth object
//        OAuth2Authorizer oauth = new OAuth2Authorizer(accessToken);
//        //create context
//        Context context = new Context(oauth, ServiceType.QBO, realmId);
//
//        // create dataservice
//        return new DataService(context);
//    }

}

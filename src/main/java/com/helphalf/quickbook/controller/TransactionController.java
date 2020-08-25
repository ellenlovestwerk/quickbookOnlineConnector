package com.helphalf.quickbook.controller;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helphalf.quickbook.client.OAuth2PlatformClientFactory;
import com.helphalf.quickbook.qbo.ContextFactory;
import com.helphalf.quickbook.qbo.DataServiceFactory;
import com.intuit.ipp.data.Error;
import com.intuit.ipp.data.Transaction;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//import com.intuit.ipp.data.RecurringTransaction;


@RestController
public class TransactionController {

    @Autowired
    OAuth2PlatformClientFactory factory;


    private static final Logger logger = Logger.getLogger(QBOController.class);

    /**
     * Sample QBO API call using OAuth2 tokens
     *
     * @param
     * @return
     */
    @ResponseBody
    @RequestMapping("/getTransactionInfo")
    public String callTransactionInfo() {

        String realmId = ContextFactory.companyID;

        if (StringUtils.isEmpty(realmId)) {
            return new JSONObject().put("response", "No realm ID.  QBO calls only work if the accounting scope was passed!").toString();
        }
        String accessToken = ContextFactory.bearerToken;
        String refreshToken = ContextFactory.refreshToken;

        String failureMsg = "Failed";
        System.out.println("accesstoken----"+accessToken);
        System.out.println("refreshToken----"+refreshToken);
        System.out.println("realmId----"+realmId);
        DataServiceFactory serviceClient = new DataServiceFactory();

        String url = factory.getPropertyValue("IntuitAccountingAPIHost") + "/v3/company";
        String result = "";
        try {

            // set custom config
            Config.setProperty(Config.BASE_URL_QBO, url);

            //get DataService
//            DataServiceFactory service = new DataServiceFactory(realmId, accessToken);
//            DataService service = getDataService(realmId, accessToken);
            DataService service = serviceClient.getDataService(realmId, accessToken);

            // get all companyinfo
            String sql = "select * Transaction";
            QueryResult queryResult = service.executeQuery(sql);
            System.out.println("queryResult---"+queryResult.getEntities());
            System.out.println("json result---"+JSON.toJSONString(queryResult));
//            result = JSON.toJSONString(queryResult);
//            return JSON.toJSONString(queryResult);
            return processResponse(failureMsg, queryResult);
//            return result;
        }
        catch (InvalidTokenException e) {
            logger.error("Error while calling executeQuery :: " + e.getMessage());

            //refresh tokens
            logger.info("received 401 during transaction call, refreshing tokens now");
            OAuth2PlatformClient client  = factory.getOAuth2PlatformClient();
//            String refreshToken = (String)session.getAttribute("refresh_token");

            try {
                BearerTokenResponse bearerTokenResponse = client.refreshToken(accessToken);
//                session.setAttribute("access_token", bearerTokenResponse.getAccessToken());
//                session.setAttribute("refresh_token", bearerTokenResponse.getRefreshToken());
                ContextFactory.bearerToken = bearerTokenResponse.getAccessToken();
                ContextFactory.refreshToken = bearerTokenResponse.getRefreshToken();

                //call company info again using new tokens
                logger.info("calling transaction using new tokens");
                DataService service = serviceClient.getDataService(realmId, accessToken);

                // get all companyinfo
                String sql = "select * from Transaction";
                QueryResult queryResult = service.executeQuery(sql);
                return processResponse(failureMsg, queryResult);

            } catch (OAuthException e1) {
                logger.error("Error while calling bearer token :: " + e.getMessage());
                return new JSONObject().put("response",failureMsg).toString();
            } catch (FMSException e1) {
                logger.error("Error while calling transaction currency :: " + e.getMessage());
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
            Transaction transaction = (Transaction) queryResult.getEntities().get(0);
            logger.info("transaction -> transaction: " + transaction.getId());
            ObjectMapper mapper = new ObjectMapper();
            try {
                String jsonInString = mapper.writeValueAsString(transaction);
                return jsonInString;
            } catch (JsonProcessingException e) {
                logger.error("Exception while getting transaction info ", e);
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

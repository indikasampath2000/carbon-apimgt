package org.wso2.carbon.apimgt.gatewaybridge.dao;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.gatewaybridge.dto.WebhookSubscriptionDTO;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.dao.constants.SQLConstants;
import org.wso2.carbon.apimgt.impl.utils.APIMgtDBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * This class represent the WebhooksSubscriptionDAO.
 */

public class WebhooksDAO {

    private static final Log log = LogFactory.getLog(WebhooksDAO.class);
    private static WebhooksDAO subscriptionsDAO;

//    /**
//     * Method to get the instance of the WebhooksSubscriptionDAO.
//     *
//     * @return {@link WebhooksDAO} instance
//     */
//    public static WebhooksDAO getInstance() {
//        if (subscriptionsDAO == null) {
//            subscriptionsDAO = new WebhooksDAO();
//        }
//        return subscriptionsDAO;
//    }

    /**
     * Manage adding Webhook Subscription to database.
     * @param webhookSubscription the DTO of webhook subscription data
     * @return a status of the operation
     * @throws APIManagementException
     */
    public boolean addSubscription(WebhookSubscriptionDTO webhookSubscription) throws APIManagementException {
        try (Connection conn = APIMgtDBUtil.getConnection()) {
            try {
                conn.setAutoCommit(false);
                int id = findSubscription(conn, webhookSubscription);
                if (id == 0) {
                    addSubscription(conn, webhookSubscription);
                } else {
                    updateSubscription(conn, webhookSubscription, id);
                }
            } catch (SQLException e) {
                handleConnectionRollBack(conn);
                throw new APIManagementException("Error while storing webhooks unsubscription request for callback" +
                        webhookSubscription.getCallback(), e);
            }
        } catch (SQLException e) {
            throw new APIManagementException("Error while storing subscription with callback " +
                    webhookSubscription.getCallback(), e);
        }
        return true;
    }

    /**
     * Check whether the subscription is available in the database.
     * @param conn          the connection of database
     * @param webhookSubscription
     * @return an integer with no of rows available
     * @throws APIManagementException
     */
    private int findSubscription(Connection conn, WebhookSubscriptionDTO webhookSubscription)
            throws APIManagementException {
        int id = 0;

        try (PreparedStatement preparedStatement = conn
                .prepareStatement(SQLConstants.ExternalGatewayWebhooksSqlConstants.FIND_SUBSCRIPTION)) {
            preparedStatement.setString(1, webhookSubscription.getSubscriberName());
            preparedStatement.setString(2, webhookSubscription.getCallback());
            preparedStatement.setString(3, webhookSubscription.getTopic());

            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt(APIConstants.Webhooks.WH_SUBSCRIPTION_ID_COLUMN);
                }
            }

        } catch (SQLException e) {
            throw new APIManagementException("Error while select existing subscriptions request for callback" +
                    webhookSubscription.getCallback() + " for the Subscriber " +
                    webhookSubscription.getSubscriberName() , e);
        }
        return id;
    }

    /**
     * Insert external gateway subscription to database.
     * @param conn
     * @param webhookSubscription
     * @throws APIManagementException
     */
    private void addSubscription(Connection conn, WebhookSubscriptionDTO webhookSubscription)
            throws APIManagementException {
        try (PreparedStatement prepareStmt = conn
                .prepareStatement(SQLConstants.ExternalGatewayWebhooksSqlConstants.ADD_SUBSCRIPTION)) {
            prepareStmt.setString(1, webhookSubscription.getSubscriberName());
            prepareStmt.setString(2, webhookSubscription.getCallback());
            prepareStmt.setString(3, webhookSubscription.getTopic());
           prepareStmt.setTimestamp(4, new Timestamp(webhookSubscription.getUpdatedTime()));
            prepareStmt.setLong(5, webhookSubscription.getExpiryTime());
            prepareStmt.setString(6, null);
            prepareStmt.setInt(7, 0);
            prepareStmt.executeUpdate();
        } catch (SQLException e) {
            throw new APIManagementException("Error while adding subscriptions request for callback" +
                    webhookSubscription.getCallback() + " for the Subscriber " +
                    webhookSubscription.getSubscriberName(), e);
        }
    }

    /**
     * Update the available gateway subscription.
     * @param conn
     * @param webhookSubscription
     * @param id
     * @throws APIManagementException
     */
    private void updateSubscription(Connection conn, WebhookSubscriptionDTO webhookSubscription, int id)
            throws APIManagementException {
        try (PreparedStatement prepareStmt = conn
                .prepareStatement(SQLConstants.WebhooksSqlConstants.UPDATE_EXISTING_SUBSCRIPTION)) {
            prepareStmt.setString(1, webhookSubscription.getCallback());
            prepareStmt.setString(2, webhookSubscription.getTopic());
            prepareStmt.setTimestamp(3,  new Timestamp(webhookSubscription.getUpdatedTime()));
            prepareStmt.setLong(4, webhookSubscription.getExpiryTime());
            prepareStmt.setInt(5, id);
            prepareStmt.executeUpdate();
        } catch (SQLException e) {
            throw new APIManagementException("Error while deleting existing subscriptions request for callback" +
                    webhookSubscription.getCallback() + " for the Subscriber " +
                    webhookSubscription.getSubscriberName(), e);
        }
    }
    /**
     * Handles the connection roll back.
     *
     * @param connection Relevant database connection that need to be rolled back.
     */
    private void handleConnectionRollBack(Connection connection) {

        try {
            if (connection != null) {
                connection.rollback();
            } else {
                log.warn("Could not perform rollback since the connection is null.");
            }
        } catch (SQLException e1) {
            log.error("Error while rolling back the transaction.", e1);
        }
    }
}

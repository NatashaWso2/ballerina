/*
*  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.ballerinalang.test.service.websub;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.awaitility.Duration;
import org.ballerinalang.test.IntegrationTestCase;
import org.ballerinalang.test.context.BallerinaTestException;
import org.ballerinalang.test.context.LogLeecher;
import org.ballerinalang.test.context.ServerInstance;
import org.ballerinalang.test.util.HttpClientRequest;
import org.ballerinalang.test.util.HttpResponse;
import org.ballerinalang.test.util.HttpsClientRequest;
import org.ballerinalang.test.util.TestConstant;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.awaitility.Awaitility.given;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This class includes an integration scenario which covers the following:
 *  1. Bringing up the Ballerina Hub
 *  2. Sending the subscription request for WebSub Subscriber services on start up, and verifying intent to subscribe,
 *      when the hub sends an intent verification request
 *  3. Functions made available to the Publishers - publishing directly on to the Ballerina Hub or to a Hub by
 *      specifying the URL (usecase: remote hubs)
 *  4. Content Delivery process - by verifying content is delivered when update notification is done for a subscribed
 *      topic - both directly to the hub and specifying hub URL
 *  5. Signature Validation for authenticated content distribution - both success and failure
 */
public class WebSubWithSecretTestCase extends IntegrationTestCase {

    private static String hubUrl = "https://localhost:9292/websub/hub";
    private static final String INTENT_VERIFICATION_SUBSCRIBER_LOG = "\"Intent verified for subscription request\"";
    private static final String INTERNAL_HUB_NOTIFICATION_SUBSCRIBER_LOG =
            "WebSub Notification Received: {\"action\":\"publish\",\"mode\":\"internal-hub\"}";
    private static final String REMOTE_HUB_NOTIFICATION_SUBSCRIBER_LOG =
            "WebSub Notification Received: {\"action\":\"publish\",\"mode\":\"remote-hub\"}";
    private static final String INTERNAL_HUB_NOTIFICATION_FROM_REQUEST_SUBSCRIBER_LOG =
            "WebSub Notification from Request: {\"action\":\"publish\",\"mode\":\"internal-hub\"}";
    private static final String REMOTE_HUB_NOTIFICATION_FROM_REQUEST_SUBSCRIBER_LOG =
            "WebSub Notification from Request: {\"action\":\"publish\",\"mode\":\"remote-hub\"}";

    private LogLeecher intentVerificationLogLeecher = new LogLeecher(INTENT_VERIFICATION_SUBSCRIBER_LOG);
    private LogLeecher internalHubNotificationLogLeecher = new LogLeecher(INTERNAL_HUB_NOTIFICATION_SUBSCRIBER_LOG);
    private LogLeecher remoteHubNotificationLogLeecher = new LogLeecher(REMOTE_HUB_NOTIFICATION_SUBSCRIBER_LOG);
    private LogLeecher internalHubNotificationFromRequestLogLeecher = new LogLeecher(
                                                                INTERNAL_HUB_NOTIFICATION_FROM_REQUEST_SUBSCRIBER_LOG);
    private LogLeecher remoteHubNotificationFromRequestLogLeecher = new LogLeecher(
                                                                REMOTE_HUB_NOTIFICATION_FROM_REQUEST_SUBSCRIBER_LOG);

    private ServerInstance ballerinaWebSubSubscriber;
    private ServerInstance ballerinaWebSubPublisher;

    @BeforeClass
    public void setup() throws BallerinaTestException, InterruptedException {
        String[] clientArgs = {new File("src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "websub" + File.separator + "websub_test_publisher.bal").getAbsolutePath(),
                "-e b7a.websub.hub.remotepublish=true", "-e test.hub.url=" + hubUrl};
        ballerinaWebSubPublisher = ServerInstance.initBallerinaServer();

        String subscriberBal = new File("src" + File.separator + "test" + File.separator + "resources"
            + File.separator + "websub" + File.separator + "websub_test_subscriber_with_secret.bal").getAbsolutePath();
        ballerinaWebSubSubscriber = ServerInstance.initBallerinaServer(8181);
        ballerinaWebSubSubscriber.addLogLeecher(intentVerificationLogLeecher);
        ballerinaWebSubSubscriber.addLogLeecher(internalHubNotificationLogLeecher);
        ballerinaWebSubSubscriber.addLogLeecher(internalHubNotificationFromRequestLogLeecher);
        ballerinaWebSubSubscriber.addLogLeecher(remoteHubNotificationLogLeecher);
        ballerinaWebSubSubscriber.addLogLeecher(remoteHubNotificationFromRequestLogLeecher);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ballerinaWebSubPublisher.runMain(clientArgs);
            } catch (BallerinaTestException e) {
                //ignored since any errors here would be reflected as test failures
            }
        });

        //Allow to bring up the hub
        given().ignoreException(ConnectException.class).with().pollInterval(Duration.FIVE_SECONDS).and()
                .with().pollDelay(Duration.TEN_SECONDS).await().atMost(60, SECONDS).until(() -> {
            HttpResponse response = HttpsClientRequest.doGet(hubUrl, ballerinaWebSubPublisher.getServerHome());
            return response.getResponseCode() == 202;
        });

        ballerinaWebSubSubscriber.startBallerinaServer(subscriberBal);

        //Allow to start up the subscriber service
        given().ignoreException(ConnectException.class).with().pollInterval(Duration.FIVE_SECONDS).and()
                .with().pollDelay(Duration.TEN_SECONDS).await().atMost(60, SECONDS).until(() -> {
            Map<String, String> headers = new HashMap<>();
            headers.put("X-Hub-Signature", "SHA256=5262411828583e9dc7eaf63aede0abac8e15212e06320bb021c433a20f27d553");
            headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_JSON);
            HttpResponse response = HttpClientRequest.doPost(
                    ballerinaWebSubSubscriber.getServiceURLHttp("websub"), "{\"dummy\":\"body\"}",
                    headers);
            return response.getResponseCode() == 202;
        });
    }

    @Test
    public void testSubscriptionAndIntentVerification() throws BallerinaTestException, InterruptedException {
        intentVerificationLogLeecher.waitForText(30000);
    }

    @Test(dependsOnMethods = "testSubscriptionAndIntentVerification")
    public void testContentReceiptForDirectHubNotification() throws BallerinaTestException {
        internalHubNotificationLogLeecher.waitForText(30000);
    }

    @Test(dependsOnMethods = "testContentReceiptForDirectHubNotification")
    public void testContentReceiptAsRequestForDirectHubNotification() throws BallerinaTestException {
        internalHubNotificationFromRequestLogLeecher.waitForText(30000);
    }

    @Test(dependsOnMethods = "testSubscriptionAndIntentVerification")
    public void testContentReceiptForRemoteHubNotification() throws BallerinaTestException {
        remoteHubNotificationLogLeecher.waitForText(30000);
    }

    @Test(dependsOnMethods = "testSubscriptionAndIntentVerification")
    public void testContentReceiptAsRequestForRemoteHubNotification() throws BallerinaTestException {
        remoteHubNotificationFromRequestLogLeecher.waitForText(30000);
    }

    @Test(dependsOnMethods = "testSubscriptionAndIntentVerification")
    public void testSignatureValidationFailure() throws BallerinaTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Hub-Signature", "SHA256=incorrect583e9dc7eaf63aede0abac8e15212e06320bb021c433a20f27d553");
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_JSON);
        HttpResponse response = HttpClientRequest.doPost(
                ballerinaWebSubSubscriber.getServiceURLHttp("websub"), "{\"dummy\":\"body\"}",
                headers);
        Assert.assertEquals(response.getResponseCode(), 404);
        Assert.assertEquals(response.getData(), "request failed: validation failed for notification");
    }

    @Test(dependsOnMethods = "testSubscriptionAndIntentVerification")
    public void testRejectionIfNoSignature() throws BallerinaTestException, IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), TestConstant.CONTENT_TYPE_JSON);
        HttpResponse response = HttpClientRequest.doPost(
                ballerinaWebSubSubscriber.getServiceURLHttp("websub"), "{\"dummy\":\"body\"}",
                headers);
        Assert.assertEquals(response.getResponseCode(), 404);
        Assert.assertEquals(response.getData(), "request failed: validation failed for notification");
    }

    @AfterClass
    private void cleanup() throws Exception {
        ballerinaWebSubPublisher.stopServer();
        ballerinaWebSubSubscriber.stopServer();
    }

}

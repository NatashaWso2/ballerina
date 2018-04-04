package packaging.push;

import ballerina/file;
import ballerina/io;
import ballerina/mime;
import ballerina/http;

function pushPackage (string accessToken, string url, string dirPath, string msg, string hostName, string port,
                      string username, string password) {
    endpoint http:ClientEndpoint httpEndpoint {
        targets: [
        {
            url: url,
            secureSocket: {
                trustStore: {
                    filePath: "${ballerina.home}/bre/security/ballerinaTruststore.p12",
                    password: "ballerina"
                },
                hostNameVerification:false,
                sessionCreation: true
            }
        }
        ],
        proxy : getProxyConfigurations(hostName, port, username, password)
    };
    mime:Entity filePart = {};
    mime:MediaType contentTypeOfFilePart = mime:getMediaType(mime:APPLICATION_OCTET_STREAM);
    filePart.contentType = contentTypeOfFilePart;
    file:File fileHandler = {path:dirPath};
    filePart.setFileAsEntityBody(fileHandler);
    mime:Entity[] bodyParts = [filePart];

    http:Request req = {};
    http:Response res = {};
    req.addHeader("Authorization", "Bearer " + accessToken);
    req.setMultiparts(bodyParts, mime:MULTIPART_FORM_DATA);
    
    var httpResponse = httpEndpoint -> post("", req);
    match httpResponse {
     http:HttpConnectorError errRes => {
         var errorResp = <error> errRes;
         match errorResp {
             error err =>  throw err;
         }
     }
     http:Response response => res = response;
    }
    if (res.statusCode != 200) {
        var jsonResponse = res.getJsonPayload();
        match jsonResponse {
            mime:EntityError errRes => {
                var errorResp = <error> errRes;
                match errorResp {
                    error err =>  throw err;
                }
            }  
            json jsonObj => io:println(jsonObj.msg.toString());            
        }
    } else {
        io:println(msg);
    }
}

function main (string[] args) {
    pushPackage(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
}

function getProxyConfigurations(string hostName, string port, string username, string password) returns (http:Proxy) {
    http:Proxy proxy = {};
    if (port != "") {
        int portInt;
        var conversion = <int> port;
        match conversion {
            error conversionErr => throw conversionErr;
            int proxyPort => portInt = proxyPort;
        }
        proxy = { host : hostName, port : portInt , userName: username, password : password };
    } else {
        proxy = {};
    }
    return proxy;
}

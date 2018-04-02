package ballerina.push;

import ballerina/file;
import ballerina/io;
import ballerina/mime;
import ballerina/http;

function pushPackage (string accessToken, string mdFileContent, string description, string homePageURL, string repositoryURL,
    string apiDocURL, string authors, string keywords, string license, string url, string dirPath, string msg) {
    endpoint http:ClientEndpoint httpEndpoint {
        targets: [
        {
            uri: url,
            secureSocket: {
                trustStore: {
                    filePath: "${ballerina.home}/bre/security/ballerinaTruststore.p12",
                    password: "ballerina"
                },
                hostNameVerification:false,
                sessionCreation: true
            }
        }
        ]
    };

    mime:Entity mdFileContentBodyPart = addStringBodyParts("description", mdFileContent);
    mime:Entity descriptionBodyPart = addStringBodyParts("summary", description);
    mime:Entity homePageURLBodyPart = addStringBodyParts("websiteURL", homePageURL);
    mime:Entity repositoryURLBodyPart = addStringBodyParts("repositoryURL", repositoryURL);
    mime:Entity apiDocURLBodyPart = addStringBodyParts("apiDocURL", apiDocURL);
    mime:Entity authorsBodyPart = addStringBodyParts("authors", authors);
    mime:Entity keywordsBodyPart = addStringBodyParts("keywords", keywords);
    mime:Entity licenseBodyPart = addStringBodyParts("license", license);

    // Artifact
    mime:Entity filePart = {};
    mime:MediaType contentTypeOfFilePart = mime:getMediaType(mime:APPLICATION_OCTET_STREAM);
    filePart.contentType = contentTypeOfFilePart;
    filePart.contentDisposition = getContentDispositionForFormData("artifact");
    file:File fileHandler = {path:dirPath};
    filePart.setFileAsEntityBody(fileHandler);
    
    mime:Entity[] bodyParts = [filePart, mdFileContentBodyPart, descriptionBodyPart, homePageURLBodyPart, repositoryURLBodyPart,
                                          apiDocURLBodyPart, authorsBodyPart, keywordsBodyPart, licenseBodyPart];

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
    pushPackage(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11]);
}

function getContentDispositionForFormData(string partName) returns (mime:ContentDisposition){
    mime:ContentDisposition contentDisposition = {};
    contentDisposition.name =  partName;
    contentDisposition.disposition = "form-data";
    return contentDisposition;
}

function addStringBodyParts (string key, string value) returns (mime:Entity) {
    mime:Entity stringBodyPart = {};
    mime:MediaType contentTypeOfStringPart = mime:getMediaType(mime:TEXT_PLAIN);
    stringBodyPart.contentType = contentTypeOfStringPart;
    stringBodyPart.contentDisposition = getContentDispositionForFormData(key);
    stringBodyPart.setText(value);
    return stringBodyPart;
}

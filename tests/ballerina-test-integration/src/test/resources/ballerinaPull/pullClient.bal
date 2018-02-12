import ballerina.net.http;
import ballerina.log;
import ballerina.compression;

function main (string[] args) {
    endpoint<http:HttpClient> httpEndpoint {
        create http:HttpClient("http://localhost:9090", {});
    }
    http:OutRequest req = {};
    http:InResponse resp = {};
    log:printInfo("Sending request");
    resp, _ = httpEndpoint.get("/echo/", req);
    compression:unzipBytes(resp.getBinaryPayload(), "/home/natasha/Documents/github_repos/ballerina/tests/ballerina-test-integration/src/test/resources/ballerinaPull/files/");
    println("Ballerina package pulled successfully");
}
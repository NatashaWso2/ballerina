import ballerina.file;
import ballerina.io;
import ballerina.net.http;

@http:configuration {
    basePath:"/echo"
}
service<http> echo {

    @http:resourceConfig {
        methods:["GET"],
        path:"/"
    }
    resource echo (http:Connection conn, http:InRequest req) {
        http:OutResponse res = {};
        string srcFilePath = "/home/natasha/Documents/github_repos/ballerina/tests/ballerina-test-integration/src/test/resources/ballerinaPull/files/hello.zip";
        io:ByteChannel src = getFileChannel(srcFilePath, "r");
        int bytesChunk = 10000;
        blob contentAsBytes = readBytes(src, bytesChunk);
        res.setBinaryPayload(contentAsBytes);
        _ = conn.respond(res);
        println("successful");
    }
}

function getFileChannel (string filePath, string permission) (io:ByteChannel) {
    file:File src = {path:filePath};
    io:ByteChannel channel = src.openChannel(permission);
    return channel;
}

function readBytes (io:ByteChannel channel, int numberOfBytes) (blob) {
    blob bytes;
    int numberOfBytesRead;
    bytes, numberOfBytesRead = channel.readBytes(numberOfBytes);
    return bytes;
}
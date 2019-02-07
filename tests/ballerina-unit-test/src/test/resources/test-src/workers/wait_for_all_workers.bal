import ballerina/io;
import ballerina/runtime;

public function testWaitForAllWorkers() {
    test();
    runtime:sleep(1000);
    io:println("Finishing Default Worker");
}

function test() {
    worker w1 {
        return;
    }

    worker w2 {
        runtime:sleep(2000);
        io:println("Finishing Worker w2");
    }
}

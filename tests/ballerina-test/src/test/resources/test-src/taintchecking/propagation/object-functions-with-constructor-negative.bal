type TestObject object {
    public {
        string testString;
    }
    new (testString) {}
    function testFunction (string input) returns string;
};

function TestObject::testFunction (string input) returns string {
    return input;
}

function secureFunction(@sensitive string secureIn, string insecureIn) {

}

function main (string... args) {
    TestObject obj = new (args[0]);
    string returnValue = obj.testFunction("staticValue");
    secureFunction(returnValue, returnValue);
}

function readLineSuccess() returns string {
    return "Hello, World!!!";
}

function testCheckedExprSemanticErrors1() returns string {
    string line = checkpanic readLineSuccess();
    return line;
}

public type MyError error<record { int code; string message?; error cause?; }>;

public type CustomError error<record { int code; string data; string message?; error cause?; }>;

function readLineInternal() returns string | int {
    return "Hello, World!!!";
}

function testCheckedExprSemanticErrors4() returns error? {
    string line = checkpanic readLineInternal();
    return ();
}

function readLineProper() returns string | MyError | CustomError {
    MyError e = error MyError("io error", code = 0);
    return e;
}

function testCheckedExprSemanticErrors5() {
    string line = checkpanic readLineProper();
}

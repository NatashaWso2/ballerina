package org.ballerinalang.packerina;

import org.ballerinalang.bre.Context;
import org.ballerinalang.launcher.util.BCompileUtil;
import org.ballerinalang.launcher.util.BRunUtil;
import org.ballerinalang.launcher.util.CompileResult;
import org.ballerinalang.model.values.BString;
import org.ballerinalang.model.values.BValue;
import org.ballerinalang.util.codegen.PackageInfo;
import org.ballerinalang.util.codegen.ProgramFile;
import org.ballerinalang.util.debugger.Debugger;
import org.ballerinalang.util.program.BLangFunctions;

import java.nio.file.Path;

import static org.ballerinalang.util.BLangConstants.USER_REPO_ARTIFACTS_DIRNAME;
import static org.ballerinalang.util.BLangConstants.USER_REPO_SRC_DIRNAME;

/**
 * Util class for network calls
 */
public class NetworkUtils {
    static CompileResult compileResult;

    /**
     * Pull/Downloads packages from the package repository
     *
     * @param resourceName
     */
    public static void pullPackages(String resourceName) {
        compileResult = compileBalFile();
        // Ballerina central repository URL
        BString host = new BString("");

        // Destination folder: by default should be downloaded to
        Path targetDirectoryPath = UserRepositoryUtils.initializeUserRepository()
                .resolve(USER_REPO_ARTIFACTS_DIRNAME)
                .resolve(USER_REPO_SRC_DIRNAME);

        BString dstPath = new BString(targetDirectoryPath + "/" + resourceName);
        BString resourcePath = new BString(resourceName);
        BValue[] paramArray = new BValue[]{host, resourcePath, dstPath};

        BRunUtil.invokeStateful(compileResult, "pullFiles", paramArray);
    }

    /**
     * Compile the bal file
     *
     * @return compile result after compiling the bal file
     */
    public static CompileResult compileBalFile() {
        CompileResult compileResult = BCompileUtil.compile(new NetworkUtils(), "src", "ballerina.pull");
        ProgramFile programFile = compileResult.getProgFile();
        PackageInfo packageInfo = programFile.getPackageInfo(compileResult.getProgFile().getEntryPkgName());
        Context context = new Context(programFile);
        Debugger debugger = new Debugger(programFile);
        programFile.setDebugger(debugger);
        compileResult.setContext(context);
        BLangFunctions.invokePackageInitFunction(programFile, packageInfo.getInitFunctionInfo(), context);
        return compileResult;
    }
}
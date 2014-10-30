import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.nio.file.*;

/**
 * The class is the core of the recommender system.
 *
 */

public class Recommender {
    String libfmPath;
    String trainPath;
    String testPath;
    String outputPath;
    int nFactors;

    public Recommender(String libfmPath, int nFactors) {
        this.libfmPath = libfmPath;
        this.nFactors = nFactors;
    }

    public void setTrainPath(String s) { this.trainPath = s; }
    public void setTestPath(String s) { this.testPath = s; }
    public void setOutputPath(String s) { this.outputPath = s; }

    /**
     * <p>First, the command used to call libFM is built.
     * Then, this command is executed with a <tt>ProcessBuilder</tt> object.
     * The program output is redirected into a <tt>BufferedReader</tt>.
     *
     * @throws IOException if something goes wrong when reading from stdout
     *                     or stderr. (unlikely)
     */
    public double run() throws IOException {
        ArrayList<String> args = new ArrayList<String>();
        args.add(libFMPath);
        args.add("-task");
        args.add("r");
        args.add("-train");
        args.add(this.trainFilePath);
        args.add("-test");
        args.add(testFilePath);
        args.add("-dim");
        args.add("'1,1," + this.nFactors + "'");

        // write predictions to outputPath if specified
        if (this.outputPath != null) {
            args.add("-out");
            args.add(outputFilePath);
        }

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        BufferedReader stdout = new BufferedReader(new InputStreamReader(pb.start().getInputStream()));

        double trainScore = 0.0, testScore = 0.0;
        String line = null;
        while ((line = stdout.readLine()) != null) {
            if (line.startsWith("#Iter")) {
                trainScore = parseRMSE(line, "Train=[0-9.]*");
                testScore = parseRMSE(line, "Test=[0-9.]*");
            }
        }
        return testScore;
    }

    double parseRMSE(String line, String patternStr) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(line);
        matcher.find();
        return Double.parseDouble(matcher.group().substring(patternStr.indexOf('=') + 1));
    }
}

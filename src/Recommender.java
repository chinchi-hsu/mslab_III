import java.io.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/**
 * The class is the core of the recommender system.
 */

public class Recommender {
    String libfmPath;
    String trainFilePath;
    String testFilePath;
    String predictionFilePath;
    int nFactors;

    public Recommender(String libfmPath, int nFactors) {
        this.libfmPath = libfmPath;
        this.nFactors = nFactors;
    }

    // call these functions and set up the file paths properly before calling run()
    public void setTrainPath(String s) { this.trainFilePath = s; }
    public void setTestPath(String s) { this.testFilePath = s; }
    public void setPredictionPath(String s) { this.predictionFilePath = s; }

    /**
     * First, the command used to call libFM is built.
     * Then, this command is executed with a <tt>ProcessBuilder</tt> object.
     *
     * @return the prediction results
     */
    public List<Double> run() throws IOException {
        // sanity check
        if (trainFilePath == null) throw new IOException("Training file not specified");
        if (testFilePath == null) throw new IOException("Testing file not specified");
        if (predictionFilePath == null) throw new IOException("Prediction file not specified");

        // build argument list
        List<String> args = new ArrayList<String>();
        args.add(libfmPath);
        args.add("-task");
        args.add("r");
        args.add("-train");
        args.add(trainFilePath);
        args.add("-test");
        args.add(testFilePath);
        args.add("-dim");
        args.add("'1,1," + nFactors + "'");
        args.add("-out");
        args.add(predictionFilePath);

        // run libFM
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectErrorStream(true);
        BufferedReader stdout = new BufferedReader(new InputStreamReader(pb.start().getInputStream()));
        String line = null;
        while ((line = stdout.readLine()) != null) {}

        // parse prediction file, return the result
        Path p = FileSystems.getDefault().getPath(predictionFilePath);
        return parsePredictionFile(p);
    }

    List<Double> parsePredictionFile(Path p) throws IOException {
        List<Double> result = new ArrayList<Double>();
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (String line : lines)
            result.add(Double.parseDouble(line.trim()));
        return result;
    }
}

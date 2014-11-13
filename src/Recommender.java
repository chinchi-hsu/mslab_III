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
    Transformation tr;

    public Recommender(String libfmPath, int nFactors, Transformation tr) {
        this.libfmPath = libfmPath;
        this.nFactors = nFactors;
        this.tr = tr;
    }

    /**
     * Set libFM training file path to <tt>s</tt>.
     * Call this function before calling <tt>run()</tt>.
     *
     * @param s The libFM training file path.
     */
    public void setTrainPath(String s) { this.trainFilePath = s; }

    /**
     * Set libFM testing file path to <tt>s</tt>.
     * Call this function before calling <tt>run()</tt>.
     *
     * @param s The libFM testing file path.
     */
    public void setTestPath(String s) { this.testFilePath = s; }

    /**
     * Set libFM output file path to <tt>s</tt>.
     * Call this function before calling <tt>run()</tt>.
     *
     * @param s The libFM output file path.
     */
    public void setPredictionPath(String s) { this.predictionFilePath = s; }

    /**
     * Run libFM for recommendation.
     * First, the command used to call libFM is built.
     * Then, this command is executed with a <tt>ProcessBuilder</tt> object.
     *
     * @return The <tt>List</tt> of predicted ratings for items in <tt>testFilePath</tt>.
     * @throws IOException      if any of the required files are not properly specified.
     * @throws RuntimeException if some other error occurs during the execution of libFM.
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
        while ((line = stdout.readLine()) != null) {
            if (line.contains("ERROR: unable to open"))
                throw new IOException("File IO Error. Please check if the file paths are valid.");
            else if (line.contains("ERROR"))
                throw new RuntimeException("Caught error from libFM. Please check the model parameters.");
        }

        // parse prediction file, return the result
        Path p = FileSystems.getDefault().getPath(predictionFilePath);
        return parsePredictionFile(p);
    }

    /**
     * Parse libFM output file, return the predictions as a <tt>List</tt> of <tt>Doubles</tt>.
     *
     * @param  p           The libFM output file path to read from.
     * @throws IOException if fails to read the prediction file.
     * @return             The prediction results.
     */
    public List<Double> parsePredictionFile(Path p) throws IOException {
        List<Double> result = new ArrayList<Double>();
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        for (String line : lines)
            result.add(Double.parseDouble(line.trim()));
        return result;
    }

    /**
     * Get the top-N recommendation item list for a given user.
     * The returned list will only contain the unseen items for the user.
     *
     * @param  user        The user ID.
     * @param  N           Only the top-<tt>N</tt> items will be returned.
     * @throws IOException if fails to read or write a file.
     * @return             A <tt>List</tt> of item IDs, sorted according to the predicted ratings.
     */
    public List<String> getRecommendationList(String user, int N) throws IOException {
        // the list of items to be returned
        List<String> recommendationList = new ArrayList<String>();

        // convert the data in this system into libFM format
        List<String> trainLines = tr.getLibfmFormatLines();
        List<String> testLines = new ArrayList<String>();
        Set<String> items = tr.getItemSet();
        for (String item : items) {
            if (tr.getRating(user, item) == null) {
                testLines.add(tr.convertToLibfmFormat(user, item));
                recommendationList.add(item);
            }
        }

        // initialize the temporary files
        Path trainPath = Files.createTempFile(null, null);
        Path testPath = Files.createTempFile(null, null);
        Path predictionPath = Files.createTempFile(null, null);
        Files.write(trainPath, trainLines, StandardCharsets.UTF_8);
        Files.write(testPath, testLines, StandardCharsets.UTF_8);

        // set file path and run recommendation
        setTrainPath(trainPath.toString());
        setTestPath(testPath.toString());
        setPredictionPath(predictionPath.toString());
        List<Double> predictions = run();

        // delete the temporary files
        Files.delete(trainPath);
        Files.delete(testPath);
        Files.delete(predictionPath);

        // sort the items according to the predicted scores
        HashMap<String, Double> ratingMap = new HashMap<String, Double>();
        for (int i = 0; i < recommendationList.size(); i++)
            ratingMap.put(recommendationList.get(i), predictions.get(i));
        Collections.sort(recommendationList, new Comparator<String>() {
            public int compare(String item1, String item2) {
                return -Double.compare(ratingMap.get(item1), ratingMap.get(item2));
            }
        });

        // if N == 0, return all items
        return (N == 0)? recommendationList: recommendationList.subList(0, N);
    }

    /**
     * Get the recommendation item list for a given user.
     * The returned list will only contain the unseen items for the user.
     *
     * @param  user        The user ID.
     * @throws IOException if fails to read or write a file.
     * @return             A <tt>List</tt> of item IDs, sorted according to the predicted ratings.
     */
    public List<String> getRecommendationList(String user) throws IOException {
        return getRecommendationList(user, 0);
    }
}

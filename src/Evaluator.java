import java.io.IOException;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Evaluator {
    static String databaseURL = "54.64.73.96:5432";
    static String databaseName = "oneclickshoppingwall";
    static String account = "ntu";
    static String password = "ntu";

    /**
     * Leave-one-out cross validation using input CSV file.
     * @see <a href="http://en.wikipedia.org/wiki/Cross-validation_%28statistics%29">"Cross-validation (statistics)" on Wikipedia</a>
     *
     * @param  libfmPath   The file path of the libFM executable.
     * @param  inputPath   The input file path
     * @param  outputPath  The file path for saving the prediction results.
     * @param  nFactors    The number of latent factors used in libFM.
     * @param  userCol     The column number of users in the input CSV file.
     * @param  itemCol     The column number of items in the input CSV file.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void loocv(String libfmPath, String inputPath, String outputPath, int nFactors, int userCol, int itemCol) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readCSVFile(inputPath, userCol, itemCol);
        List<String> lines = tr.getLibfmFormatLines();

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,item_id,answer,prediction");

        // start leave-one-out cross validation
        for (int testIdx = 0; testIdx < lines.size(); testIdx++) {
            // parse the prediction target line
            String testLine = lines.get(testIdx);
            int rating = Integer.parseInt(testLine.split(" ")[0]);
            int userIdx = Integer.parseInt(testLine.split(" ")[1].split(":")[0]); // user index in libFM
            int itemIdx = Integer.parseInt(testLine.split(" ")[2].split(":")[0]); // item index in libFM
            String user = tr.mapUserIDToName(userIdx); // user number in the input CSV file
            String item = tr.mapItemIDToName(itemIdx); // item number in the input CSV file
            System.out.print("\rNow testing: user=" + user + " item=" + item + "...");

            // create a list of training instances and a list of testing instances
            List<String> trainList = new ArrayList<String>();
            List<String> testList = new ArrayList<String>();
            testList.add(testLine);
            for (int i = 0; i < lines.size(); i++) {
                if (i == testIdx) continue;
                trainList.add(lines.get(i));
            }

            // initialize the temporary files
            Path trainPath = Files.createTempFile(null, null);
            Path testPath = Files.createTempFile(null, null);
            Path predictionPath = Files.createTempFile(null, null);
            Files.write(trainPath, trainList, StandardCharsets.UTF_8);
            Files.write(testPath, testList, StandardCharsets.UTF_8);

            // run recommendation
            Recommender model = new Recommender(libfmPath, nFactors, tr);
            model.setTrainPath(trainPath.toString());
            model.setTestPath(testPath.toString());
            model.setPredictionPath(predictionPath.toString());

            // get the prediction result
            List<Double> predictions = model.run();
            output.add(user+","+item+","+rating+","+predictions.get(0));

            // delete the temporary files
            Files.delete(trainPath);
            Files.delete(testPath);
            Files.delete(predictionPath);
        }

        // write the predictions results to a file
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
        System.out.println("\nRMSE = " + rmse(outputPath));
    }

    /**
     * Leave-one-out cross validation using remote DB data.
     * @see <a href="http://en.wikipedia.org/wiki/Cross-validation_%28statistics%29">"Cross-validation (statistics)" on Wikipedia</a>
     *
     * @param  libfmPath         The file path of the libFM executable.
     * @param  tableName         which table in the DB contains the data.
     * @param  outputPath        The file path for saving the prediction results.
     * @param  nFactors          The number of latent factors used in libFM.
     * @param  userField         The data field corresponding to user ID.
     * @param  itemField         The data field corresponding to item ID.
     * @param  categoryTableName The name of a table in the database like "master_category".
     * @param  itemTableName     The name of a table in the database like "product" or "coupon".
     * @param  categoryField     The field representing categories like "mcid".
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void loocv_db(String libfmPath, String tableName, String outputPath, int nFactors, String userField, String itemField, String categoryTableName, String itemTableName, String categoryField) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        if (categoryTableName == null || itemTableName == null || categoryField == null)
            tr.readDatabase(databaseURL, databaseName, account, password, tableName, userField, itemField); // don't use category
        else
            tr.readDatabase(databaseURL, databaseName, account, password, tableName, userField, itemField, categoryTableName, itemTableName, categoryField);
        List<String> lines = tr.getLibfmFormatLines();

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,item_id,answer,prediction");

        // start leave-one-out cross validation
        for (int testIdx = 0; testIdx < lines.size(); testIdx++) {
            // parse the prediction target line
            String testLine = lines.get(testIdx);
            int rating = Integer.parseInt(testLine.split(" ")[0]);
            int userIdx = Integer.parseInt(testLine.split(" ")[1].split(":")[0]); // user index in libFM
            int itemIdx = Integer.parseInt(testLine.split(" ")[2].split(":")[0]); // item index in libFM
            String user = tr.mapUserIDToName(userIdx); // user number in the input CSV file
            String item = tr.mapItemIDToName(itemIdx); // item number in the input CSV file
            System.out.print("\rNow testing: user=" + user + " item=" + item + "...");

            // create a list of training instances and a list of testing instances
            List<String> trainList = new ArrayList<String>();
            List<String> testList = new ArrayList<String>();
            testList.add(testLine);
            for (int i = 0; i < lines.size(); i++) {
                if (i == testIdx) continue;
                trainList.add(lines.get(i));
            }

            // initialize the temporary files
            Path trainPath = Files.createTempFile(null, null);
            Path testPath = Files.createTempFile(null, null);
            Path predictionPath = Files.createTempFile(null, null);
            Files.write(trainPath, trainList, StandardCharsets.UTF_8);
            Files.write(testPath, testList, StandardCharsets.UTF_8);

            // run recommendation
            Recommender model = new Recommender(libfmPath, nFactors, tr);
            model.setTrainPath(trainPath.toString());
            model.setTestPath(testPath.toString());
            model.setPredictionPath(predictionPath.toString());

            // get the prediction result
            List<Double> predictions = model.run();
            output.add(user+","+item+","+rating+","+predictions.get(0));

            // delete the temporary files
            Files.delete(trainPath);
            Files.delete(testPath);
            Files.delete(predictionPath);
        }

        // write the predictions results to a file
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
        System.out.println("\nRMSE = " + rmse(outputPath));
    }

    /**
     * Leave-one-out cross validation using remote DB data (without category information).
     * @see <a href="http://en.wikipedia.org/wiki/Cross-validation_%28statistics%29">"Cross-validation (statistics)" on Wikipedia</a>
     *
     * @param  libfmPath         The file path of the libFM executable.
     * @param  tableName         which table in the DB contains the data.
     * @param  outputPath        The file path for saving the prediction results.
     * @param  nFactors          The number of latent factors used in libFM.
     * @param  userField         The data field corresponding to user ID.
     * @param  itemField         The data field corresponding to item ID.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void loocv_db(String libfmPath, String tableName, String outputPath, int nFactors, String userField, String itemField) throws IOException {
        loocv_db(libfmPath, tableName, outputPath, nFactors, userField, itemField, null, null, null);
    }

    /**
     * Compute RMSE for an output file.
     * @see <a href="https://www.kaggle.com/wiki/RootMeanSquaredError">The mathematical definition of RMSE</a>
     *
     * @param  filepath    file path where the prediction results are saved
     * @throws IOException if error occurs when reading the file
     * @return             The RMSE computed from the prediction results.
     */
    public static double rmse(String filepath) throws IOException {
        final int answerCol = 2;
        final int predictionCol = 3;
        double s = 0.0;
        Path p = FileSystems.getDefault().getPath(filepath);
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        lines.remove(0); //remove the header line
        for (String line : lines)
            s += Math.pow(Double.parseDouble(line.split(",")[answerCol]) - Double.parseDouble(line.split(",")[predictionCol]), 2);
        return Math.sqrt(s / lines.size());
    }

    /**
     * Recommend a top-N list of items for each user using data from CSV file.
     *
     * @param  libfmPath   The file path of the libFM executable.
     * @param  inputPath   The input file path
     * @param  outputPath  The file path for saving the prediction results.
     * @param  nFactors    The number of latent factors used in libFM.
     * @param  userCol     The column number of users in the input CSV file.
     * @param  itemCol     The column number of items in the input CSV file.
     * @param  N           The number of items to be recommended to this user.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void recommendTopNForUsers(String libfmPath, String inputPath, String outputPath, int nFactors, int userCol, int itemCol, int N) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readCSVFile(inputPath, userCol, itemCol);

        // initialize the Recommender
        Recommender model = new Recommender(libfmPath, nFactors, tr);

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,items");

        // get recommendation list for each user
        List<String> recommendationList = null;
        String line = null;
        for (String user : tr.getUserSet()) {
            System.out.print("\rGenerating the top-" + N + " recommendation list for user=" + user + "...");
            recommendationList = model.getRecommendationList(user, N);
            if (recommendationList.isEmpty())
                continue;
            line = "" + user + ",";
            for (String item : recommendationList)
                line += item + " ";
            output.add(line.trim());
        }

        System.out.println("\nOutput the recommendation lists...");
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    /**
     * Recommend a list of items for each user using data from CSV file.
     *
     * @param  libfmPath   The file path of the libFM executable.
     * @param  inputPath   The input file path
     * @param  outputPath  The file path for saving the prediction results.
     * @param  nFactors    The number of latent factors used in libFM.
     * @param  userCol     The column number of users in the input CSV file.
     * @param  itemCol     The column number of items in the input CSV file.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void recommendForUsers(String libfmPath, String inputPath, String outputPath, int nFactors, int userCol, int itemCol) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readCSVFile(inputPath, userCol, itemCol);

        // initialize the Recommender
        Recommender model = new Recommender(libfmPath, nFactors, tr);

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,items");

        // get recommendation list for each user
        List<String> recommendationList = null;
        String line = null;
        for (String user : tr.getUserSet()) {
            System.out.print("\rGenerating the recommendation list for user=" + user + "...");
            recommendationList = model.getRecommendationList(user);
            if (recommendationList.isEmpty())
                continue;
            line = "" + user + ",";
            for (String item : recommendationList)
                line += item + " ";
            output.add(line.trim());
        }

        System.out.println("\nOutput the recommendation lists...");
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    /**
     * Recommend a top-N list of items for each user using data from DB.
     *
     * @param  libfmPath   The file path of the libFM executable.
     * @param  tableName   which table in the DB contains the data.
     * @param  outputPath  The file path for saving the prediction results.
     * @param  nFactors    The number of latent factors used in libFM.
     * @param  userField   The data field corresponding to user ID.
     * @param  itemField   The data field corresponding to item ID.
     * @param  N           The number of items to be recommended to this user.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void recommendTopNForUsers_db(String libfmPath, String tableName, String outputPath, int nFactors, String userField, String itemField, int N) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readDatabase(databaseURL, databaseName, account, password, tableName, userField, itemField);

        // initialize the Recommender
        Recommender model = new Recommender(libfmPath, nFactors, tr);

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,items");

        // get recommendation list for each user
        List<String> recommendationList = null;
        String line = null;
        for (String user : tr.getUserSet()) {
            System.out.print("\rGenerating the top-" + N + " recommendation list for user=" + user + "...");
            recommendationList = model.getRecommendationList(user, N);
            if (recommendationList.isEmpty())
                continue;
            line = "" + user + ",";
            for (String item : recommendationList)
                line += item + " ";
            output.add(line.trim());
        }

        System.out.println("\nOutput the recommendation lists...");
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    /**
     * Recommend a list of items for each user using data from DB.
     *
     * @param  libfmPath   The file path of the libFM executable.
     * @param  tableName   which table in the DB contains the data.
     * @param  outputPath  The file path for saving the prediction results.
     * @param  nFactors    The number of latent factors used in libFM.
     * @param  userField   The data field corresponding to user ID.
     * @param  itemField   The data field corresponding to item ID.
     *
     * @throws IOException if error occurs at file IO.
     */
    public static void recommendForUsers_db(String libfmPath, String tableName, String outputPath, int nFactors, String userField, String itemField) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readDatabase(databaseURL, databaseName, account, password, tableName, userField, itemField);

        // initialize the Recommender
        Recommender model = new Recommender(libfmPath, nFactors, tr);

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,items");

        // get recommendation list for each user
        List<String> recommendationList = null;
        String line = null;
        for (String user : tr.getUserSet()) {
            System.out.print("\rGenerating the recommendation list for user=" + user + "...");
            recommendationList = model.getRecommendationList(user);
            if (recommendationList.isEmpty())
                continue;
            line = "" + user + ",";
            for (String item : recommendationList)
                line += item + " ";
            output.add(line.trim());
        }

        System.out.println("\nOutput the recommendation lists...");
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    /**
     * Recommend a list of items for each category and each user using data from DB.
     *
     * @param  libfmPath         The file path of the libFM executable.
     * @param  tableName         which table in the DB contains the rating data.
     * @param  outputPath        The file path for saving the prediction results.
     * @param  nFactors          The number of latent factors used in libFM.
     * @param  userField         The data field corresponding to user ID.
     * @param  itemField         The data field corresponding to item ID.
     * @param  categoryTableName The name of a table in the database like "master_category".
     * @param  itemTableName     The name of a table in the database like "product".
     * @param  categoryField     The field representing categories like "mcid".
     * @throws IOException       if error occurs at file IO.
     */
    public static void recommendForUsers_db_category(String libfmPath, String tableName, String outputPath, int nFactors, String userField, String itemField, String categoryTableName, String itemTableName, String categoryField) throws IOException {
        // load input CSV file and transform into libFM format
        Transformation tr = new Transformation();
        tr.readDatabase(databaseURL, databaseName, account, password, tableName, userField, itemField, categoryTableName, itemTableName, categoryField);

        // initialize the Recommender
        Recommender model = new Recommender(libfmPath, nFactors, tr);

        // lines to be written to the output file
        List<String> output = new ArrayList<String>();
        output.add("user_id,category,items");

        // get recommendation list for each user
        List<String> recommendationList = null;
        String line = null;
        long startTime = System.nanoTime();
        for (String user : tr.getUserSet()) {
            for (String category : tr.getCategorySet()) {
                System.out.print("\rGenerating the recommendation list for user=" + user + " category=" + category + "...");
                recommendationList = model.getRecommendationList(user, category);
                if (recommendationList.isEmpty())
                    continue;
                line = "" + user + "," + category + ",";
                for (String item : recommendationList)
                    line += item + " ";
                output.add(line.trim());
            }
        }
        System.out.println();
        System.out.println("Execution time: " + 1.0 * (System.nanoTime() - startTime) / 1e9);
        System.out.println("Execution time per user: " + 1.0 * (System.nanoTime() - startTime) / 1e9 / tr.getUserSet().size());

        System.out.println("Output the recommendation lists...");
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        if (args.length < 7) {
            System.out.println("\nERROR: number of argument is wrong. Please see the README file.\n\n");
            System.exit(-1);
        }
        try {
            if (args[6].equals("-db")) {
                if (args.length == 7) {
                    // no category
                    loocv_db(args[0], args[1], args[2], Integer.parseInt(args[3]), args[4], args[5]);
                    recommendTopNForUsers_db(args[0], args[1], args[2]+".toplist", Integer.parseInt(args[3]), args[4], args[5], 5);
                    recommendForUsers_db(args[0], args[1], args[2]+".list", Integer.parseInt(args[3]), args[4], args[5]);
                }
                else {
                    // use category
                    loocv_db(args[0], args[1], args[2], Integer.parseInt(args[3]), args[4], args[5], args[7], args[8], args[9]);
                    recommendForUsers_db_category(args[0], args[1], args[2]+".category", Integer.parseInt(args[3]), args[4], args[5], args[7], args[8], args[9]);
                }
            }
            else {
                loocv(args[0], args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
                recommendTopNForUsers(args[0], args[1], args[2]+".toplist", Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), 5);
                recommendForUsers(args[0], args[1], args[2]+".list", Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

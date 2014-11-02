import java.io.IOException;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Evaluator {
    /**
     * Leave-one-out cross validation.
     * @see <a href="http://en.wikipedia.org/wiki/Cross-validation_%28statistics%29">"Cross-validation (statistics)" on Wikipedia</a>
     *
     * @param  libfmPath   libFM executable file path
     * @param  inputPath   input file path
     * @param  outputPath  file path for saving the prediction results
     * @param  nFactors    number of latent factors used in libFM
     * @param  userCol     the column number of users in the input CSV file
     * @param  itemCol     the column number of items in the input CSV file
     *
     * @throws IOException if error occurs at file IO
     */
    static void loocv(String libfmPath, String inputPath, String outputPath, int nFactors, int userCol, int itemCol) throws IOException {
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
            String userNumber = tr.mapUserIDToName(userIdx); // user number in the input CSV file
            String itemNumber = tr.mapItemIDToName(itemIdx); // item number in the input CSV file

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
            Recommender model = new Recommender(libfmPath, nFactors);
            model.setTrainPath(trainPath.toString());
            model.setTestPath(testPath.toString());
            model.setPredictionPath(predictionPath.toString());

            // get the prediction result
            List<Double> predictions = model.run();
            output.add(userNumber+","+itemNumber+","+rating+","+predictions.get(0));

            // delete the temporary files
            Files.delete(trainPath);
            Files.delete(testPath);
            Files.delete(predictionPath);
        }

        // write the predictions results to a file
        Files.write(FileSystems.getDefault().getPath(outputPath), output, StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
        if (args.length != 6) {
            System.out.println("\nUSAGE: java -cp .:lib:bin Evaluator LIBFMPATH INPUTPATH OUTPUTPATH NFACTORS USERCOL ITEMCOL\n\n");
            System.exit(-1);
        }
        try {
            loocv(args[0], args[1], args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

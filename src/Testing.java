import java.io.IOException;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

public class Testing {
    private static double crossValidation(String inputFilePath, int nFolds, int nFactors) throws IOException {
        List<Double> scores = new ArrayList<Double>();
        List<String> lines = Files.readAllLines(Paths.get(inputFilePath), StandardCharsets.UTF_8);
        Collections.shuffle(lines);

        int nLines = lines.size();
        if (nFolds == 0) nFolds = nLines;

        System.out.println("answer,prediction");

        int stepSize = (int)Math.ceil(nLines / (double)nFolds);
        for (int fromIndex = 0; fromIndex < nLines; fromIndex += stepSize) {
            List<String> trainList = new ArrayList<String>();
            List<String> testList = new ArrayList<String>();
            int toIndex = (fromIndex + stepSize > nLines)? nLines: fromIndex + stepSize;
            for (int l = 0; l < nLines; l++) {
                if (l >= fromIndex && l < toIndex) {
                    String line = lines.get(l);
                    System.out.print(line.split(" ")[0] + ",");
                    testList.add(line);
                }
                else
                    trainList.add(lines.get(l));
            }

            Path trainPath = Files.createTempFile(null, null);
            Files.write(trainPath, trainList, StandardCharsets.UTF_8);
            trainPath.toFile().deleteOnExit();

            Path testPath = Files.createTempFile(null, null);
            Files.write(testPath, testList, StandardCharsets.UTF_8);
            testPath.toFile().deleteOnExit();

            Recommender model = new Recommender(trainPath.toString(), nFactors);
            double score = model.run(testPath.toString(), "temp");
            scores.add(score);

            Path path = FileSystems.getDefault().getPath("temp");
            System.out.println(Files.readAllLines(path).get(0));
        }

        double sum = 0;
        for (double d : scores)
            sum += d;
        return sum / scores.size();
    }

    public static void main(String[] args) {
        String inputFilePath = args[0];
        int nFactors = Integer.parseInt(args[1]);
        int nFolds = Integer.parseInt(args[2]);
        double score = 0.0;
        try {
            score = crossValidation(inputFilePath, nFolds, nFactors);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (nFolds == 0) // LOO-CV
            System.out.println("LOO-CV RMSE = " + score);
        else
            System.out.println("" + nFolds + "-fold CV RMSE = " + score);
    }
}


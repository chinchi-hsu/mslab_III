import java.util.*;
import java.io.*;
import au.com.bytecode.opencsv.*;

public class Transformation{
	public static void main(String[] argv) throws Exception{
		String csvFilePath = argv[0];
		String outputFilePath = argv[1];
		int userColumnIndex = Integer.parseInt(argv[2]);
		int itemColumnIndex = Integer.parseInt(argv[3]);

		CSVHandler csvHandler = new CSVHandler(csvFilePath, outputFilePath);
		csvHandler.readCSVFile(userColumnIndex, itemColumnIndex);

		csvHandler.remapUserNumbers();
		csvHandler.remapItemNumbers();
		if(argv.length > 4){
			csvHandler.readFeatures(argv[4]);
		}
		csvHandler.constructRatings();
		csvHandler.writeOutputFile();
	}
}

class CSVHandler{
	private String csvFilePath;
	private String outputFilePath;
	ArrayList<String> users;
	ArrayList<String> items;
	HashMap<String, String> userMap;
	HashMap<String, String> itemMap;
	HashMap<String, String> featureMap;
	HashMap<String, HashMap<String, Integer> > ratingMap;
	
	public CSVHandler(String csvFilePath, String outputFilePath){
		this.csvFilePath = csvFilePath;
		this.outputFilePath = outputFilePath;

		this.users = new ArrayList<String>();
		this.items = new ArrayList<String>();
		this.userMap = new HashMap<String, String>();
		this.itemMap = new HashMap<String, String>();
		this.featureMap = new HashMap<String, String>();
		this.ratingMap = new HashMap<String, HashMap<String, Integer> >();
	}

	public void readCSVFile(int userColumnIndex, int itemColumnIndex) throws Exception{
		this.users.clear();
		this.items.clear();

		CSVReader csvReader = new CSVReader(new FileReader(csvFilePath));
		String[] row = null;
		boolean isHeader = true;

		while((row = csvReader.readNext()) != null){
			if(isHeader){
				isHeader = false;
				continue;
			}

			String user = row[userColumnIndex];
			String item = row[itemColumnIndex];
			if(user.equals("-1") || user.equals("") || item.equals("-1") || item.equals("")){
				continue;
			}

			users.add(user);
			items.add(item);
		}

		csvReader.close();
	}

	public void constructRatings(){
		this.ratingMap.clear();

		for(int i = 0; i < this.users.size(); i ++){
			String user = this.users.get(i);
			String item = this.items.get(i);

			if(!this.ratingMap.containsKey(user)){
				this.ratingMap.put(user, new HashMap<String, Integer>());
			}
			if(!this.ratingMap.get(user).containsKey(item)){
				this.ratingMap.get(user).put(item, 0);
			}

			int rating = this.ratingMap.get(user).get(item);
			this.ratingMap.get(user).put(item, rating + 1);
		}
	}

	public void remapUserNumbers(){
		this.remapListNumbers(this.users, this.userMap);
	}
	
	public void remapItemNumbers(){
		this.remapListNumbers(this.items, this.itemMap, this.userMap.size()+1);
	}

	public void readFeatures(String featureFilePath) throws Exception{
		CSVReader csvReader = new CSVReader(new FileReader(featureFilePath));
		String[] row = null;
		boolean isHeader = true;

		while((row = csvReader.readNext()) != null){
			if(isHeader){
				isHeader = false;
				continue;
			}

			String item = itemMap.get(row[0]), s = "";
			for(int i = 1; i < row.length; i ++){
				s += (itemMap.size()+userMap.size()+i) + ":" + row[i];
			}
			this.featureMap.put(item, s);
		}
	}

	public void writeOutputFile() throws Exception{
		PrintWriter writer = new PrintWriter(outputFilePath);

		for(Map.Entry<String, HashMap<String, Integer> > userKey: this.ratingMap.entrySet()){
			String user = userKey.getKey();
			HashMap<String, Integer> itemMap = userKey.getValue();

			for(Map.Entry<String, Integer> itemKey: itemMap.entrySet()){
				String item = itemKey.getKey();
				Integer rating = itemKey.getValue();

				writer.printf("%d %s:1 %s:1", rating, user, item);
				if(featureMap.get(item) != null){
					writer.printf(" " + featureMap.get(item));
				}
				writer.printf("\n");
			}
		}

		writer.close();
	}

	private void remapListNumbers(ArrayList<String> list, HashMap<String, String> idMap){
		remapListNumbers(list, idMap, 1);
	}

	private void remapListNumbers(ArrayList<String> list, HashMap<String, String> idMap, int startFrom){
		ArrayList<String> copiedList = new ArrayList<String>();
		
		for(String element: list){
			copiedList.add(element);
		}

		Collections.sort(copiedList);
		idMap.clear();

		int counter = startFrom;
		for(int i = 0; i < copiedList.size(); i ++){
			if(i > 0 && !copiedList.get(i - 1).equals(copiedList.get(i))){
				counter ++;
			}
			idMap.put(copiedList.get(i), Integer.toString(counter));
		}

		for(int i = 0; i < list.size(); i ++){
			String element = list.get(i);
			list.set(i, idMap.get(element));
		}
	}
}

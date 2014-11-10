import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import au.com.bytecode.opencsv.*;
import java.sql.*;

public class Transformation{
	private HashMap<String, Integer> userNameIDMap;
	private HashMap<String, Integer> itemNameIDMap;
	private HashMap<Integer, String> userIDNameMap;
	private HashMap<Integer, String> itemIDNameMap;
	private HashMap<String, HashMap<String, Integer> > ratingMap;
	
	public Transformation(){
		this.userNameIDMap = new HashMap<String, Integer>();
		this.itemNameIDMap = new HashMap<String, Integer>();
		this.userIDNameMap = new HashMap<Integer, String>();
		this.itemIDNameMap = new HashMap<Integer, String>();
		this.ratingMap = new HashMap<String, HashMap<String, Integer> >();
	}

	/* Reads user-item information from an CSV file, and constructs a mapping between read names and IDs.
	 * 	Name: The original information (name, id, etc.) of users or items from the CSV file
	 * 	ID: The transformed (remapped) integers in this class
	 * @param csvFilePath		The path of the assigned CSV file.
	 * @param userColumnIndex	Assigns which column index represents users.
	 * @param itemColumnIndex	Assigns which column index represents items.
	 */
	public void readCSVFile(String csvFilePath, int userColumnIndex, int itemColumnIndex) throws IOException {
		this.clearMappings();

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

			this.userNameIDMap.put(user, 0);	// adds to the user list
			this.itemNameIDMap.put(item, 0);	// adds to the item list
			this.addRating(user, item);		// adds the rating of the user to the item
		}

		csvReader.close();

		this.constructMaps(this.userNameIDMap, this.userIDNameMap);
		this.constructMaps(this.itemNameIDMap, this.itemIDNameMap, this.userIDNameMap.size());
	}

	/* Reads user-item information from a PostgreSQL database, and constructs a mapping between read names and IDs
	 * @param databaseURL		The URL (consisting of the port number) of the assigned PostgreSQL database like "54.64.73.96:5432".
	 * @param databaseName		The name of the assigned database like "oneclickshoppingwall".
	 * @param account		An account to log in the database.
	 * @param password		The password of the account.
	 * @param tableName		The name of a table in the assigned database like "product_order".
	 * @param userField		The field representing users like "aid".
	 * @param itemField		The field representing items like "pid".
	 */
	public void readDatabase(String databaseURL, String databaseName, String account, String password, String tableName, String userField, String itemField){
		this.clearMappings();

		Connection connection = null;
		Statement statement = null;
		ResultSet resultSet = null;
		String databaseFullURL =  "jdbc:postgresql://" + databaseURL + "/" + databaseName;			// forms the full url accepted by the JDBC library
		String query = "SELECT \"" + userField + "\", \"" + itemField + "\" FROM \"" + tableName + "\"";	// sets the query cammand
		
		try{
			// Connects the database
			connection = DriverManager.getConnection(databaseFullURL, account, password);
			statement = connection.createStatement();
			resultSet = statement.executeQuery(query);

			// Gets users and items
			while(resultSet.next()){
				String user = resultSet.getString(userField);
				String item = resultSet.getString(itemField);
				
				if(user.equals("-1") || user.equals("") || item.equals("-1") || item.equals("")){
					continue;
				}

				this.userNameIDMap.put(user, 0);	// adds to the user list
				this.itemNameIDMap.put(item, 0);	// adds to the item list
				this.addRating(user, item);		// adds the rating of the user to the item
			}
			
			statement.close();
			connection.close();
		}
		catch(SQLException ex){
			ex.printStackTrace();
			System.exit(1);
		}
		
		this.constructMaps(this.userNameIDMap, this.userIDNameMap);
		this.constructMaps(this.itemNameIDMap, this.itemIDNameMap, this.userIDNameMap.size());
	}

	public void writeOutputFile(String outputFilePath) throws IOException {
		Files.write(FileSystems.getDefault().getPath(outputFilePath), getLibfmFormatLines(), StandardCharsets.UTF_8);
	}

	public List<String> getLibfmFormatLines() {
		List<String> lines = new ArrayList<String>();
		for(Map.Entry<String, HashMap<String, Integer> > userKey: this.ratingMap.entrySet()){
		    String user = userKey.getKey();
		    HashMap<String, Integer> itemMap = userKey.getValue();
		    for(Map.Entry<String, Integer> itemKey: itemMap.entrySet()){
			String item = itemKey.getKey();
			Integer rating = itemKey.getValue();
			int remappedUser = this.mapUserNameToID(user);
			int remappedItem = this.mapItemNameToID(item);
			lines.add(String.format("%d %d:1 %d:1", rating, remappedUser, remappedItem));
		    }
		}
		return lines;
	}

	public int mapUserNameToID(String name){
		return this.userNameIDMap.get(name);
	}
	
	public int mapItemNameToID(String name){
		return this.itemNameIDMap.get(name);
	}

	public String mapUserIDToName(int ID){
		return this.userIDNameMap.get(ID);
	}

	public String mapItemIDToName(int ID){
		return this.itemIDNameMap.get(ID);
	}

	private void clearMappings(){
		this.userNameIDMap.clear();
		this.itemNameIDMap.clear();
		this.userIDNameMap.clear();
		this.itemIDNameMap.clear();
		this.ratingMap.clear();
	}

	/* Gives an unique integer ID to every name in the list.
	 * @param nameIDMap	A mapping from names to IDs.
	 * @param IDNameMap	A mapping from IDs to names.
	 * @param startFrom	The starting index of the ID counter.
	 */
	private void constructMaps(HashMap<String, Integer> nameIDMap, HashMap<Integer, String> IDNameMap, int startFrom){
		TreeSet<String> sortedNames = new TreeSet<String>(nameIDMap.keySet());
		int IDCounter = startFrom;
		for(String name: sortedNames){
			nameIDMap.put(name, IDCounter);
			IDNameMap.put(IDCounter, name);
			IDCounter ++;
		}
	}

	/* Gives an unique integer ID to every name in the list where the starting index of the ID counter is 0.
	 * @param nameIDMap A mapping from names to IDs.
	 * @param IDNameMap A mapping from IDs to names.
	 */
	private void constructMaps(HashMap<String, Integer> nameIDMap, HashMap<Integer, String> IDNameMap){
        	constructMaps(nameIDMap, IDNameMap, 0);
    	}

	/* Adds the rating of a user to an item.
	 * @param user The name of a user.
	 * @param item The name of an item.
	 */
	private void addRating(String user, String item){
		if(!this.ratingMap.containsKey(user)){
			this.ratingMap.put(user, new HashMap<String, Integer>());
		}
		if(!this.ratingMap.get(user).containsKey(item)){
			this.ratingMap.get(user).put(item, 0);
		}

		int rating = this.ratingMap.get(user).get(item);
		this.ratingMap.get(user).put(item, rating == 5? 5: rating + 1); // saturate at 5 to avoid noisy data
	}
}

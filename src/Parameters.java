

public class Parameters {

	private static String PathToSave= "//192.168.90.203/Logistyka/Listy";
	private static String PathToSaveReports= "//192.168.90.203/Logistyka/Raporty";
	
	public static String getPathToSave(){
		return PathToSave;
	}
	
	public static String getPathToSaveReports(){
		return PathToSaveReports;
	}
	
	public static void setPathToSave (String s){
		PathToSave = s;
	}
}

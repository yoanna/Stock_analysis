
import java.sql.*;

import javax.swing.*;
public class DBConnection {
	static String adresSerwera = "192.168.90.123";
	Connection conn=null;
	public static Connection dbConnector()
	{
		try {
			
			Class.forName("org.mariadb.jdbc.Driver");
			Connection conn=DriverManager.getConnection("jdbc:mariadb://"+adresSerwera+"/fatdb","listy","listy1234");
			//JOptionPane.showMessageDialog(null, "Connection Successful");
			return conn;
		}catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, e);
			return null;
		}
	}
}

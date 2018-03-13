import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Raport{
		
		String sql;
		String tytul;
		int wiersze;
		String [][] dane;
		String [] header;
		
		public Raport(String sql, String tytul) {
			this.sql = sql;
			if(tytul.endsWith(".csv")) {
				this.tytul = tytul;
			}
			else this.tytul = tytul+".csv";
			
			try {
				countRows();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void countRows() throws SQLException {
			String sql_temp = "Select count(*) from ( "+sql+" ) T";
			Connection conn = DBConnection.dbConnector();
			Statement st_temp = conn.createStatement();
			ResultSet rs_temp = st_temp.executeQuery(sql_temp);
			while(rs_temp.next()) this.wiersze = rs_temp.getInt(1);
			rs_temp.close();st_temp.close();conn.close();
		}

		public String getSql() {
			return sql;
		}

		public int getWiersze() {
			return wiersze;
		}

		public String[][] getDane() {
			return dane;
		}

		public void setDane(String[][] dane) {
			this.dane = dane;
		}

		public String[] getHeader() {
			return header;
		}

		public void setHeader(String[] header) {
			this.header = header;
		}
		
		public void generateDane() throws SQLException {
			dane = new String[header.length][wiersze];
			System.out.println("Ilosc kolumn: "+header.length);
			System.out.println(sql);
			Connection conn = DBConnection.dbConnector();
			Statement st01 = conn.createStatement();
			ResultSet rs01 = st01.executeQuery(sql);
			int j = 0;
			while(rs01.next()) {
				for(int i = 0; i<header.length; i++) {
					String data = rs01.getString(i+1);
					if(data==null) data = "";
					dane[i][j] = data.replace(';', ',');
				}
				j++;
			}
			rs01.close();st01.close();conn.close();
		}

		public String getTytul() {
			return tytul;
		}
	}
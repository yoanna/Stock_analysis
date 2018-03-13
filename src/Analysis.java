import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Analysis {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		try {
			run_analysis();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void run_analysis() throws SQLException {
		
		Calendar a = Calendar.getInstance();
		SimpleDateFormat godz = new SimpleDateFormat("HH:mm");
		System.out.println(godz.format(a.getTime())+" start");
		
		//pobiera cale zapotrzebowanie na podstawie bonow magazynowych 
		try {
			checkStorenotes();
		}catch (SQLException e){
			System.out.println("SQL Error, brak analizy projektow produkcyjnych");
		}
		
		//dobiera zapotrzebowanie z zamowien zewnetrznych
		try {
			checkExternalOrders();
		}catch (SQLException e){
			System.out.println("SQL Error, brak analizy projektow produkcyjnych");
		}
		
		a = Calendar.getInstance();
		System.out.println(godz.format(a.getTime())+" Zacznij analizê");
		//zaczyna analize na 1 poziomie zapotrzebowania
		check(1);
		a = Calendar.getInstance();
		System.out.println(godz.format(a.getTime())+" koniec");
		//wykonaj raporty
		report();
	}
	
	public static void checkStorenotes() throws SQLException {
		
		Calendar a = Calendar.getInstance();
		SimpleDateFormat godz = new SimpleDateFormat("HH:mm");
		
		Connection conn = DBConnection.dbConnector();
		String czyscLevel_analysis = "Delete from level_analysis";
		Statement czysc = conn.createStatement();
		czysc.executeUpdate(czyscLevel_analysis);
		czysc.close();		
		
		/*String sql = "select storenotesdetail.artikelcode, storenotesdetail.bostdeh, storenotesdetail.BESTELEENHEID from storenotesdetail " + 
				"where storenotesdetail.afdeling <= 300 and storenotesdetail.bostdeh <> 0  order by afdeling";*/
		//pobierz zapotrzebowanie z bonow magazynowych
		String sql = "select storenotesdetail.artikelcode, sum(storenotesdetail.bostdeh) as bostdeh, storenotesdetail.BESTELEENHEID from storenotesdetail "
				+ "	where storenotesdetail.afdeling <= 300 and storenotesdetail.bostdeh <> 0  "
				+ "group by artikelcode, besteleenheid "
				+ "order by ARTIKELCODE";
		
		Statement st01 = conn.createStatement();
		ResultSet rs01 = st01.executeQuery(sql);
		while(rs01.next()) {
			String kod_art = rs01.getString("artikelcode");
			a = Calendar.getInstance();
			System.out.println(godz.format(a.getTime())+" analiza artykulu = "+kod_art);
			double ile = rs01.getDouble("bostdeh");
			String jednostka = rs01.getString("besteleenheid");
			String jedn_glowna = "";
			
			// sprawdz czy podstawowa jednostka
				//jeœli nie to przelicz
			String sql02 = "select eenheid from artikel_alteenh where artikelcode = '"+kod_art+"' and standaard = 1 ";
			Statement st02 = conn.createStatement();
			ResultSet rs02 = st02.executeQuery(sql02);
			while(rs02.next()) {
				jedn_glowna = rs02.getString("eenheid");
			} rs02.close(); st02.close();
			 if(!jednostka.equals(jedn_glowna)) {
				 /*String sql03 = "select hoeveelheid from artikel_alteenh where artikelcode = '"+kod_art+"' and eenheid = '"+jednostka+"'";
				 Statement st03 = conn.createStatement();
				 ResultSet rs03 = st03.executeQuery(sql03);
				 while(rs03.next()) {
					 ile = ile*rs03.getDouble("hoeveelheid");
				 } rs03.close(); st03.close(); */
				 jednostka = jedn_glowna;
			 }
			 
			// sprawdz ile na stocku
			 String sql04 = "Select * from stock_analysis where kod_artykulu = '"+kod_art+"'";
			 Statement st04 = conn.createStatement();
			 ResultSet rs04 = st04.executeQuery(sql04);
			 double stock = 0;
			 while(rs04.next()) {
				 stock = rs04.getDouble("ilosc");
			 }
			 rs04.close(); st04.close();
			 
			 //jeœli stock >= ile -> zmien stock, idz dalej
			 if(stock>=ile) {
				 String sql_update01 = "Update stock_analysis set ilosc = "+(stock-ile)+" where kod_artykulu = '"+kod_art+"'";
				 Statement st05 = conn.createStatement();
				 st05.executeUpdate(sql_update01);
				 st05.close();
			 }
			 
			 //jeœli stock < ile -> zeruj stock, dodaj artykul do zapotrzebowania wtornego
			 else { 
				 //zeruj stock, pomniejsz ile
				 ile = ile - stock;
				 String sql_update01 = "Update stock_analysis set ilosc = 0 where kod_artykulu = '"+kod_art+"'";
				 Statement st05 = conn.createStatement();
				 st05.executeUpdate(sql_update01);
				 st05.close();
				 
			 		//sprawdz czy w zapotrzebowaniu wtornym istnieje artykul
				 boolean istnieje = false;
				 double ilosc_zapotrzebowania = 0;
				 String sql06 = "Select * from level_analysis where kod_artykulu = '"+kod_art+"' and level = 1";
				 Statement st06 = conn.createStatement();
				 ResultSet rs06 = st06.executeQuery(sql06);
				 while(rs06.next()){
					 istnieje = true;
					 ilosc_zapotrzebowania = rs06.getDouble("ilosc");
				 }
				 rs06.close();st06.close();
				 
			 			//jesli nie to stworz i wpisz zapotrzebowanie (ile-stock)
				 if(!istnieje) {
					 String sql_insert01 = "Insert into level_analysis values (1, '"+kod_art+"', "+ile+", '"+jednostka+"')";
					 Statement st07 = conn.createStatement();
					 st07.executeUpdate(sql_insert01);
					 st07.close();
				 }
			 			//jesli tak to updateuj rekord z dodana iloscia +(ile-stock)
				 else {
					 String sql_update02 = "Update level_analysis set ilosc = "+(ilosc_zapotrzebowania+ile)+" where kod_artykulu = '"+kod_art+"' and level = 1";
					 Statement st08 = conn.createStatement();
					 st08.executeUpdate(sql_update02);
					 st08.close();
				 }
				 
			 }
		}
		rs01.close();
		st01.close();
		conn.close();
	}
	public static void checkExternalOrders() throws SQLException {
		
		Connection conn = DBConnection.dbConnector();
		Calendar a = Calendar.getInstance();
		SimpleDateFormat godz = new SimpleDateFormat("HH:mm");
		//wez wszystkie zamowienia produkcyjne z dzialu 4 i 14 (zewnetrzne i serwisowe) i porownaj je z zamowieniami w dziale sprzedaz (zeby nie duplikowac zapotrzebowania)
		String sql = "select bestelling.LEVERANCIERORDERNUMMER, bestellingdetail.ARTIKELCODE from bestelling " + 
				"left join bestellingdetail on bestelling.leverancier = bestellingdetail.leverancier and bestelling.ORDERNUMMER = bestellingdetail.ORDERNUMMER " + 
				"where bestelling.leverancier in (4, 14) and bestellingdetail.ARTIKELCODE like '%/%' and bestelling.STATUSCODE = 'O'";
		
		Statement st01 = conn.createStatement();
		ResultSet rs01 = st01.executeQuery(sql);
		while(rs01.next()) {
			//kod artykulu w projektach 4 i 14 zawiera referencje do zamowienia zewn.
			String [] referencja = rs01.getString(2).split("-");
			String []p = referencja[1].split("/");
			
			//jezeli projekt z grupy 4
			if(rs01.getString(1).startsWith("4/")) {
				if(p.length>2) {
					//szukaj po artykule zamowionym
					String sql02 = "update external_orders_analysis "
							+ "join verkoopdetail on verkoopdetail.klantnr = external_orders_analysis.klantnr and verkoopdetail.bestellingnr = external_orders_analysis.bestellingnr "
							+ "set external_orders_analysis.wykorzystano = 1 "
							+ "where verkoopdetail.ARTIKELCODE = '"+rs01.getString(2)+"'";
					Statement st02 = conn.createStatement();
					int i = st02.executeUpdate(sql02);
					st02.close();
					if(i==0) {
						System.out.println(rs01.getString(1)+" "+rs01.getString(2));
					}
				}
				else {
					//szukaj po referencji
					String sql02 = "update external_orders_analysis "
							+ "join verkoop on verkoop.klantnr = external_orders_analysis.klantnr and verkoop.bestellingnr = external_orders_analysis.bestellingnr "
							+ "set external_orders_analysis.wykorzystano = 1 "
							+ "where verkoop.referentie like '%"+referencja[referencja.length-1]+"%'";
					Statement st02 = conn.createStatement();
					int i = st02.executeUpdate(sql02);
					st02.close();
					if(i==0) {
						System.out.println(rs01.getString(1)+" "+referencja[referencja.length-1]);
					}
				}
			}
			//jezeli projekt z grupy 14 (serwis)
			else {
				//szybki update dla zamowienia zewn wypisanego w artykule zamowienia wewn
				String sql02 = "Update  external_orders_analysis "
						+ "set external_orders_analysis.wykorzystano = 1 "
						+ "where concat(external_orders_analysis.klantnr, \"/\", external_orders_analysis.bestellingnr) = '"+referencja[referencja.length-1]+"'";
				Statement st02 = conn.createStatement();
				int i = st02.executeUpdate(sql02);
				st02.close();
				if(i==0) {
					System.out.println(rs01.getString(1)+" "+referencja[referencja.length-1]);
				}
			}
		}
		rs01.close();
		st01.close();
		
		//pobierz pozostale zamowienia zewnetrzne
		String sql00 = "select verkoopdetail.klantnr, verkoopdetail.BESTELLINGNR, verkoopdetail.artikelcode, verkoopdetail.besteld - verkoopdetail.geleverd as ilosc, verkoopdetail.BESTELEENHEID, artikel_algemeen.VERSCHAFFINGSCODE from external_orders_analysis " + 
				"left join verkoopdetail on verkoopdetail.klantnr = external_orders_analysis.klantnr and verkoopdetail.bestellingnr = external_orders_analysis.bestellingnr " + 
				"left join artikel_algemeen on verkoopdetail.ARTIKELCODE = artikel_algemeen.ARTIKELCODE " + 
				"where external_orders_analysis.wykorzystano = 0 and (verkoopdetail.besteld - verkoopdetail.geleverd) <> 0 and verkoopdetail.artikelcode <> 'M' and artikel_algemeen.VERSCHAFFINGSCODE not in ('F', 'C')";
		
		Statement st00 = conn.createStatement();
		ResultSet rs00 = st00.executeQuery(sql00);
		while(rs00.next()) {
			String kod_art = rs00.getString("artikelcode");
			a = Calendar.getInstance();
			System.out.println(godz.format(a.getTime())+" analiza artykulu = "+kod_art);
			double ile = rs00.getDouble("ilosc");
			String jednostka = rs00.getString("besteleenheid");
			String jedn_glowna = "";
			
			// sprawdz czy podstawowa jednostka
				//jeœli nie to przelicz
			String sql02 = "select eenheid from artikel_alteenh where artikelcode = '"+kod_art+"' and standaard = 1 ";
			Statement st02 = conn.createStatement();
			ResultSet rs02 = st02.executeQuery(sql02);
			while(rs02.next()) {
				jedn_glowna = rs02.getString("eenheid");
			} rs02.close(); st02.close();
			 if(!jednostka.equals(jedn_glowna)) {
				 String sql03 = "select hoeveelheid from artikel_alteenh where artikelcode = '"+kod_art+"' and eenheid = '"+jednostka+"'";
				 Statement st03 = conn.createStatement();
				 ResultSet rs03 = st03.executeQuery(sql03);
				 while(rs03.next()) {
					 ile = ile*rs03.getDouble("hoeveelheid");
				 } rs03.close(); st03.close(); 
				 jednostka = jedn_glowna;
			 }
			 
			// sprawdz ile na stocku
			 String sql04 = "Select * from stock_analysis where kod_artykulu = '"+kod_art+"'";
			 Statement st04 = conn.createStatement();
			 ResultSet rs04 = st04.executeQuery(sql04);
			 double stock = 0;
			 while(rs04.next()) {
				 stock = rs04.getDouble("ilosc");
			 }
			 rs04.close(); st04.close();
			 
			 //jeœli stock >= ile -> zmien stock, idz dalej
			 if(stock>=ile) {
				 String sql_update01 = "Update stock_analysis set ilosc = "+(stock-ile)+" where kod_artykulu = '"+kod_art+"'";
				 Statement st05 = conn.createStatement();
				 st05.executeUpdate(sql_update01);
				 st05.close();
			 }
			 
			 //jeœli stock < ile -> zeruj stock, dodaj artykul do zapotrzebowania wtornego
			 else { 
				 //zeruj stock, pomniejsz ile
				 ile = ile - stock;
				 String sql_update01 = "Update stock_analysis set ilosc = 0 where kod_artykulu = '"+kod_art+"'";
				 Statement st05 = conn.createStatement();
				 st05.executeUpdate(sql_update01);
				 st05.close();
				 
			 		//sprawdz czy w zapotrzebowaniu wtornym istnieje artykul
				 boolean istnieje = false;
				 double ilosc_zapotrzebowania = 0;
				 String sql06 = "Select * from level_analysis where kod_artykulu = '"+kod_art+"' and level = 1";
				 Statement st06 = conn.createStatement();
				 ResultSet rs06 = st06.executeQuery(sql06);
				 while(rs06.next()){
					 istnieje = true;
					 ilosc_zapotrzebowania = rs06.getDouble("ilosc");
				 }
				 rs06.close();st06.close();
				 
			 			//jesli nie to stworz i wpisz zapotrzebowanie (ile-stock)
				 if(!istnieje) {
					 String sql_insert01 = "Insert into level_analysis values (1, '"+kod_art+"', "+ile+", '"+jednostka+"')";
					 Statement st07 = conn.createStatement();
					 st07.executeUpdate(sql_insert01);
					 st07.close();
				 }
			 			//jesli tak to updateuj rekord z dodana iloscia +(ile-stock)
				 else {
					 String sql_update02 = "Update level_analysis set ilosc = "+(ilosc_zapotrzebowania+ile)+" where kod_artykulu = '"+kod_art+"' and level = 1";
					 Statement st08 = conn.createStatement();
					 st08.executeUpdate(sql_update02);
					 st08.close();
				 }
				 
			 }
		}
		rs01.close();
		st01.close();
		conn.close();
		
	}
	
	
	public static void check(int i) throws SQLException {
		//sprawdza zapotrzebowanie + idzie poziomami w dol
		Calendar a = Calendar.getInstance();
		SimpleDateFormat godz = new SimpleDateFormat("HH:mm");
		System.out.println(godz.format(a.getTime())+" Poziom "+i);
		
		Connection conn = DBConnection.dbConnector();
		boolean kolejny_poziom = false;
		int kolejny_level = i+1;
		
		String sql01 = "Select * from level_analysis where level = "+i;
		Statement st01 = conn.createStatement();
		ResultSet rs01 = st01.executeQuery(sql01);
		while(rs01.next()) {
			String kod_art = rs01.getString("kod_artykulu");
			double ile = rs01.getDouble("ilosc");
			String jednostka = rs01.getString("jednostka");
			a = Calendar.getInstance();
			System.out.println("   "+godz.format(a.getTime())+"  "+kod_art);
			//sprawdz czy zapotrzebowanie da siê pokryæ stanem
			String sql04 = "Select * from stock_analysis where kod_artykulu = '"+kod_art+"'";
			 Statement st04 = conn.createStatement();
			 ResultSet rs04 = st04.executeQuery(sql04);
			 double stock = 0;
			 while(rs04.next()) {
				 stock = rs04.getDouble("ilosc");
			 }
			 rs04.close(); st04.close();
			 
			//jeœli tak to zmieñ stan, zmieñ zapotrzebowanie
			 if(stock>=ile) {
				 String sql_update01 = "Update stock_analysis set ilosc = "+(stock-ile)+" where kod_artykulu = '"+kod_art+"'";
				 String sql_update02 = "Update level_analysis set ilosc = 0 where level = "+i+" and kod_artykulu = '"+kod_art+"'" ;
				 Statement st05 = conn.createStatement();
				 st05.executeUpdate(sql_update01);
				 st05.executeUpdate(sql_update02);
				 st05.close();
			 }
				//jeœli nie to znajdz zamowienie/a, które pokryje to zapotrzebowanie 
			 else {
				 //wyzeruj stock jesli cos zostalo
				 if(stock>0) {
					 String sql_update03 = "Update stock_analysis set ilosc = 0 where kod_artykulu = '"+kod_art+"'";
					 String sql_update04 = "Update level_analysis set ilosc = "+(ile-stock)+" where level = "+i+" and kod_artykulu = '"+kod_art+"'" ;
					 Statement st05 = conn.createStatement();
					 st05.executeUpdate(sql_update03);
					 st05.executeUpdate(sql_update04);
					 st05.close();
					 ile=ile-stock;
				 }
				 
				 //zmienna 'ile' -> ilosc artykulu do pokrycia
				
				String sql02 = "Select * from orders_analysis where wykorzystano <> 1 and kod_artykulu = '"+kod_art+"' order by data asc, dostawca asc, zamowienie asc";
				Statement st02 = conn.createStatement();
				ResultSet rs02 = st02.executeQuery(sql02);
				while(rs02.next()) {
					String dostawca = rs02.getString("dostawca");
					String nrZam = rs02.getString("zamowienie");
					String jedn_zamowienia = rs02.getString("jednostka");
					double ile_Zam = rs02.getDouble("ilosc_poz");
					a = Calendar.getInstance();
					System.out.println("      "+godz.format(a.getTime())+"  "+dostawca+"/"+nrZam);
					//status 
					//jesli 0 -> zamówienie jeszcze nie poddane analizie
					//jeœli 1 -> wykorzystane ca³e
					//jeœli 2 -> czêœæ zamówienia zosta³o ju¿ wykorzystane
					int status = rs02.getInt("wykorzystano");
					String jedn_glowna = "";
					// sprawdz czy podstawowa jednostka
						//jeœli nie to przelicz
					String sql07 = "select eenheid from artikel_alteenh where artikelcode = '"+kod_art+"' and standaard = 1 ";
					Statement st07 = conn.createStatement();
					ResultSet rs07 = st07.executeQuery(sql07);
					while(rs07.next()) {
						jedn_glowna = rs07.getString("eenheid");
					} rs07.close(); st07.close();
					if(!jedn_zamowienia.equals(jedn_glowna)) {
						String sql03 = "select hoeveelheid from artikel_alteenh where artikelcode = '"+kod_art+"' and eenheid = '"+jedn_zamowienia+"'";
						Statement st03 = conn.createStatement();
						ResultSet rs03 = st03.executeQuery(sql03);
						while(rs03.next()) {
							ile_Zam = ile_Zam*rs03.getDouble("hoeveelheid");
						} rs03.close(); st03.close(); 
						jedn_zamowienia = jedn_glowna;
					}
					
					//je¿eli wykorzystujemy ca³e zamówienie:
					if(ile_Zam<=ile) {
						//wykorzystane zamówienia oznacz jako wykorzystane;
						String sql_update05 = "Update orders_analysis set wykorzystano = 1, ilosc_poz = 0 where dostawca = "+dostawca+" and zamowienie = "+nrZam+" and kod_artykulu = '"+kod_art+"'";
						Statement st_up = conn.createStatement();
						st_up.executeUpdate(sql_update05);
						st_up.close();
						ile = ile-ile_Zam;
					}
					//je¿eli pokryjemy zapotrzebowanie z niecalego zamowienia:
					else {
						// te które jeszcze nie zosta³y do koñca wykorzystane to oznacz ze jeszcze pozostalo do wykorzystania.
						String sql_update05 = "Update orders_analysis set wykorzystano = 2, ilosc_poz = "+(ile_Zam-ile)+" where dostawca = "+dostawca+" and zamowienie = "+nrZam+" and kod_artykulu = '"+kod_art+"'";
						Statement st_up = conn.createStatement();
						st_up.executeUpdate(sql_update05);
						st_up.close();
						ile = 0;
						
					}
					//jeœli wykorzystane zamówienia s¹ zamówieniami 500 produkcyjnymi - sprawdz czy nie nalezy pokryc bonow magazynowych (statys == 0). jeœli robiony jest insert oznacz flagê kolejnego poziomu.
					if(dostawca.equals("500") && status == 0) {
						String sql08 = "Select storenotesdetail.artikelcode, bostdeh, artikel_alteenh.eenheid from storenotesdetail join artikel_alteenh on artikel_alteenh.artikelcode = storenotesdetail.artikelcode where afdeling = '"+dostawca+"' and afdelingseq = '"+nrZam+"' and bostdeh <> 0 and standaard = 1";
						Statement st08 = conn.createStatement();
						ResultSet rs08 = st08.executeQuery(sql08);
						while(rs08.next()) {
							double ilosc_2 = rs08.getDouble("bostdeh");
							String kod_art_2 = rs08.getString("artikelcode");
							 //sprawdz czy dany artykul jest juz w zamowieniu w kolejnym levelu
							boolean jest = false;
							String sql_check01 = "Select * from level_analysis where level = "+(kolejny_level)+" and kod_artykulu = '"+kod_art_2+"'";
							Statement check01 = conn.createStatement();
							ResultSet rs_check01 = check01.executeQuery(sql_check01);
							while(rs_check01.next()) {
								jest = true;
								double ilosc_3 = rs_check01.getDouble("ilosc");
								String sql_update06 = "Update level_analysis set ilosc = "+(ilosc_3 + ilosc_2)+" where level = "+(kolejny_level)+" and kod_artykulu = '"+kod_art_2+"'";
								Statement st_up2 = conn.createStatement();
								st_up2.executeUpdate(sql_update06);
								st_up2.close();
								
							}rs_check01.close(); check01.close();
							
							//jeœli nie 	
							if(!jest) {
								kolejny_poziom = true;
								String sql_insert01 = "Insert into level_analysis values ("+kolejny_level+", '"+rs08.getString("artikelcode")+"', "+rs08.getDouble("bostdeh")+", '"+rs08.getString("eenheid")+"')";
								Statement st_up3 = conn.createStatement();
								st_up3.executeUpdate(sql_insert01);
								st_up3.close();
							}
						}
						rs08.close();st08.close();
					}
					
					//je¿eli mamy ca³e zapotrzebowanie, skoñcz pêtlê z zamówieniami
					if(ile==0) {
						break;
					}
				}
				rs02.close();st02.close();
				// zmodyfikuj zapotrzebowanie
				String sql_update07 = "Update level_analysis set ilosc = "+ile+" where level = "+i+" and kod_artykulu = '"+kod_art+"'";
				Statement st05 = conn.createStatement();
				st05.executeUpdate(sql_update07);
				st05.close();
			 }
		}
		rs01.close();st01.close();
		conn.close();
		if(kolejny_poziom) {
			check(kolejny_level);
		}
	}
	
	public static void report() throws SQLException {
		String path = "//192.168.90.203/Logistyka/Raporty";
		CSVFileWriter eksport = new CSVFileWriter(path);
		Calendar a = Calendar.getInstance();
		SimpleDateFormat godz = new SimpleDateFormat("yyyy-MM-dd");
		System.out.println(godz.format(a.getTime())+" start");
		
		//niepotrzebne zamowienia
		String[] header_4 = new String[] {"Dostawca", "Nr dostawcy", "Nr zamowienia", "Kod artykulu", "Ilosc", "Jednostka", "Kwota", "Waluta"};
		String sql_04 = "select leverancier.NAAM, dostawca, zamowienie, kod_artykulu, ilosc_poz, jednostka, bestellingdetail.SUMA, bestellingdetail.MUNT from orders_analysis  " + 
				"join bestelling on bestelling.leverancier = orders_analysis.dostawca and bestelling.ORDERNUMMER = orders_analysis.zamowienie " + 
				"join bestellingdetail on bestellingdetail.leverancier = orders_analysis.dostawca and bestellingdetail.ORDERNUMMER = orders_analysis.zamowienie and bestellingdetail.ARTIKELCODE = orders_analysis.kod_artykulu " + 
				"join leverancier on leverancier.LEVERANCIERNR = orders_analysis.dostawca " + 
				"where wykorzystano = 0 and dostawca > 500 and (bestelling.afdeling in (0, 7, null)) order by dostawca, zamowienie, kod_artykulu";
		Raport zamowienia_zewn = new Raport(sql_04, "Niepotrzebne zamowienia zewnetrzne");
		zamowienia_zewn.setHeader(header_4);
		zamowienia_zewn.generateDane();
		eksport.nowyPlik(zamowienia_zewn.getTytul(), ";", zamowienia_zewn.getHeader(), zamowienia_zewn.getWiersze(), zamowienia_zewn.getDane());
		
		
		//niepotrzebne serie 500
		
		String[] header_1 = new String[] {"Kod artykulu", "Nazwa artykulu", "Ilosc", "Jednostka", "Numery serii"};
		String sql_01 = "select kod_artykulu, artikel_algemeen.OMSCHRIJVING as nazwa_artykulu, sum(ilosc_poz) as ilosc, jednostka, group_concat(zamowienie SEPARATOR ', ') from orders_analysis " + 
				"join artikel_algemeen on artikel_algemeen.ARTIKELCODE = orders_analysis.kod_artykulu " + 
				"where wykorzystano = 0 and dostawca = 500 group by kod_artykulu order by sum(ilosc_poz) desc";
		
		Raport serie500 = new Raport(sql_01, "Niepotrzebne_500");
		serie500.setHeader(header_1);
		serie500.generateDane();
		eksport.nowyPlik(serie500.getTytul(), ";", serie500.getHeader(), serie500.getWiersze(), serie500.getDane());
		
		//stan magazynowy szczegolowo
			// z podzialem na grupy
		String sql_02 = "select * from ("
				+ "select stock_analysis.kod_artykulu, artikel_algemeen.omschrijving, stock_analysis.ilosc, stock_analysis.jednostka, artikel_algemeen.ARTIKELGROEP, artikel_kostprijs.CFKOSTPRIJS, artikel_kostprijs.CFFIRMAMUNT, stock_analysis.ilosc*artikel_kostprijs.CFKOSTPRIJS as suma from stock_analysis "
				+ "join artikel_kostprijs on artikel_kostprijs.ARTIKELCODE = stock_analysis.kod_artykulu  "
				+ "join artikel_algemeen on artikel_algemeen.ARTIKELCODE = stock_analysis.kod_artykulu  "
				+ "where stock_analysis.ilosc > 0 "
				+ "group by stock_analysis.kod_artykulu "
				+ "order by datum desc ) T order by T.artikelgroep asc, T.kod_artykulu";
		String[] header_2 = new String[] {"Kod artykulu", "Nazwa artykulu", "Ilosc", "Jednostka", "Grupa art", "Koszt jednostkowy", "Waluta", "Koszt calkowity"};
		Raport stock = new Raport(sql_02, "Stock");
		stock.setHeader(header_2);
		stock.generateDane();
		eksport.nowyPlik(stock.getTytul(), ";", stock.getHeader(), stock.getWiersze(), stock.getDane());
		
		//stan wg grup artykulow
		
		String sql_03 = "select T2.artikelgroep, artikel_groep.OMSCHRIJVING, sum(T2.suma), T2.cffirmamunt from "
				+ "( select * from "
					+ "(select stock_analysis.* , artikel_algemeen.ARTIKELGROEP, artikel_kostprijs.CFKOSTPRIJS, artikel_kostprijs.CFFIRMAMUNT, stock_analysis.ilosc*artikel_kostprijs.CFKOSTPRIJS as suma from stock_analysis "
					+ "join artikel_kostprijs on artikel_kostprijs.ARTIKELCODE = stock_analysis.kod_artykulu " + 
					"join artikel_algemeen on artikel_algemeen.ARTIKELCODE = stock_analysis.kod_artykulu " + 
					"where stock_analysis.ilosc > 0 " +
					"group by stock_analysis.kod_artykulu " + 
					"order by datum desc ) T order by T.artikelgroep asc, T.kod_artykulu ) T2 " + 
					"join artikel_groep on T2.artikelgroep = artikel_groep.ARTIKELGROEP " + 
					"group by T2.artikelgroep";
		String[] header_3 = new String[] {"Kod grupy", "Nazwa grupy", "Suma", "Waluta"};
		Raport stockGrupy = new Raport(sql_03, "StanwgGrup");
		stockGrupy.setHeader(header_3);
		stockGrupy.generateDane();
		eksport.nowyPlik(stockGrupy.getTytul(), ";", stockGrupy.getHeader(), stockGrupy.getWiersze(), stockGrupy.getDane());
	}
	
	
}

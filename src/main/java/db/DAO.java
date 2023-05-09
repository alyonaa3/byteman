package db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.h2.tools.RunScript;

import entity.Mitarbeiter;

public class DAO {

  private final String dbPath = "./db";
  private final String db = "jdbc:h2:" + dbPath + "/mitarbeiter"; // H2 embedded
  private Connection con;
  private Statement stmt;

  public void connect() {
    try {
      Class.forName("org.h2.Driver").getDeclaredConstructor().newInstance();
      this.con = DriverManager.getConnection(db);
      this.con.setAutoCommit(false);
      this.stmt = this.con.createStatement();
      List<String> tabellen = this.berechneTabellen();
      if (!tabellen.contains("MITARBEITER")) {
        if (!this.skriptLaden("Mitarbeiter.sql")) {
          System.err.println("SQL-Skript nicht gefunden");
        }
      } else {
        int max = 0;
        try (ResultSet rs2 = this.stmt.executeQuery(
            "SELECT MAX(MITARBEITER.Minr) FROM MITARBEITER")) {
          rs2.next();
          max = rs2.getInt(1);
        } catch (SQLException ex) {
          System.err.println("SQL-Fehler: " + ex);
        }
        Mitarbeiter.setMicount(max + 1);
      }

    } catch (Exception ex) {
      System.err
          .println("JDBC-Treiber nicht gefunden/nutzbar: " + ex);
    } 
  }

  private List<String> berechneTabellen() {
    var tabellen = new ArrayList<String>();
    try {
      ResultSet rs = this.con.getMetaData()
          .getTables(null, null, "%", null);
      while (rs.next()) {
        if (rs.getString(4).equals("TABLE")) {
          tabellen.add(rs.getString(3));
        }
      }
      rs.close();
    } catch (SQLException ex) {
      System.err.println("DB-Problem: " + ex);
    }
    return tabellen;
  }

  /**
   * Loescht die gesamte Datenbank (alle Dateien).
   */
  public void resetAll() {
    this.deleteDir(new File(this.dbPath));
  }

  /**
   * Loescht rekursiv ein Verzeichnis mit allen Unterverzeichnissen.
   * 
   * @param path File-Objekt mit zu loescherndfem Verzeichnis
   */
  private void deleteDir(File path) {
    for (File file : path.listFiles()) {
      if (file.isDirectory())
        deleteDir(file);
      file.delete();
    }
    path.delete();
  }

  /**
   * Ermoeglicht die Ausfuehrung eines SQL-Skripts in datei.
   * 
   * @param datei Datei, die SQL-Skript enthaelt.
   * @return war das Laden erfolgreich?
   */
  private boolean skriptLaden(String datei) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream("./" + datei), "UTF-8"))) {
      RunScript.execute(this.con, reader);
    } catch (Exception e) {
      System.err
          .println("Problem beim Lesen von " + datei + "\n" + e);
      return false;
    }
    return true;
  }

  /**
   * Schliesst die Datenbank.
   */
  public void dbSchliessen() {
    if (this.con != null) {
      try {
        this.con.close();
      } catch (SQLException ex) {
        System.err.println("Problem DB zu schliessen.");
      }
    }
  }

  /**
   * Sucht nach einem Mitarbeiter mit der Nummer nr und gibt ihn in einem Optional
   * zurueck, das leer ist, falls so ein Objekt nicht existiert
   * 
   * @param nr Nummer des gesuchten Mitarbeiters
   * @return in Optional verpackter Mitarbeiter, das leer sein kann
   */
  public Optional<Mitarbeiter> find(int nr) {
    try (ResultSet rs = this.stmt.executeQuery(
        "SELECT * FROM MITARBEITER WHERE MITARBEITER.Minr=" + nr)) {
      if (rs.next()) {
        Mitarbeiter ma = new Mitarbeiter(rs.getString(2), null);
        ma.setMinr(rs.getInt(1));

        ResultSet rs2 = this.stmt.executeQuery(
            "SELECT CHEFVON.Cminr FROM CHEFVON WHERE CHEFVON.Minr="
                + nr);
        if (rs2.next()) { // dann gibt es einen Chef
          int chefid = rs2.getInt(1);
          ma.setChef(this.find(chefid).get());
        }
        return Optional.of(ma);
      }
      this.con.commit();
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    return Optional.empty();
  }

  /**
   * Berechnet die Nummern aller Mitarbeiter, von denen nr der direkte Chef ist.
   * 
   * @param nr Minr des Mitarbeiters, dessen Untergebene berechnet werden
   * @return Liste der Mitarbeiternummern der Untergebenen
   */
  private List<Integer> chefVon(int nr) {
    var erg = new ArrayList<Integer>();
    try (ResultSet rs = this.stmt.executeQuery(
        "SELECT * FROM CHEFVON WHERE CHEFVON.Cminr=" + nr)) {
      while (rs.next()) {
        erg.add(rs.getInt(1));
      }
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    return erg;
  }

  /**
   * Es wird versucht ein Mitarbeiterobjekt als neuen Mitarbeiter in die Datenbank
   * einzutragen. Dabei darf der angegebene Chef danach weiterhin maximal drei
   * Mitarbeiter betreuen und der Mitarbeiter nicht sein eigener Chef sein.
   * 
   * @param ma einzutragendes Mitarbeiter-Objekt
   * @return war Eintragung erfolgreich?
   */
  public boolean mitarbeiterHinzu(Mitarbeiter ma) {
    if (ma.getChef() != null
        && this.chefVon(ma.getChef().getMinr()).size() > 2) {
      throw new IllegalArgumentException(
          ma.getChef() + " hat zu viele Mitarbeiter");
    }
//    if (ma.getChef() != null && ma.getMinr() == ma.getChef().getMinr()){
//      throw new IllegalArgumentException("niemand ist sein eigener Chef");
//    }
    try {
      this.stmt
          .execute("INSERT INTO MITARBEITER VALUES(" + ma.getMinr()
              + ",'" + ma.getName() + "')");
      if (ma.getChef() != null) {
        this.stmt.execute("INSERT INTO CHEFVON VALUES(" + ma.getMinr()
            + "," + ma.getChef().getMinr() + ")");
      }
      this.con.commit();
      return true;
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    return false;
  }

  /**
   * Berechnet eine Liste aller Mitarbeiterobjekte, die in der Datenbank
   * gespeichert sind
   * 
   * @return alle Mitarbeiter
   */
  public List<Mitarbeiter> alle() {
    var erg = new ArrayList<Mitarbeiter>();
    // System.out.println("stmt: " + stmt);
    var tmp = new ArrayList<Integer>();
    try (ResultSet rs = this.stmt.executeQuery(
        "SELECT * FROM MITARBEITER")) {
      while (rs.next()) {
        tmp.add(rs.getInt(1));
      }
      con.commit();
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    for (var i : tmp) {
      erg.add(this.find(i).get());
    }
    return erg;
  }

  /**
   * Loescht vollstaendig einen Mitarbeiter mit der Mitarbeiternummer nr. Sollte
   * er Chef sein, werden auch diese Eintraege geloescht.
   * 
   * @param nr Minr des zu loeschenden Mitarbeiters
   * @return gibt es keinen Mitarbeiter mit Nummer nr mehr?
   */
  public boolean loeschen(int nr) {
    var ma = this.find(nr);
    if (ma.isEmpty()) {
      return true;
    }
    try {
      this.stmt.execute("DELETE FROM CHEFVON WHERE CHEFVON.Minr ="
          + ma.get().getMinr() + " OR CHEFVON.Cminr ="
          + ma.get().getMinr());
      this.stmt
          .execute("DELETE FROM MITARBEITER WHERE MITARBEITER.Minr ="
              + ma.get().getMinr());
      this.con.commit();
      return true;
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    return false;
  }

  /**
   * Prueft ob beu einer Aenderung des Chefs von Mitarbeiter mit der Nummer von
   * auf die Nummer neu dann von direkt oder ueber andere Chef von sich selbst
   * werden wuerde.
   * 
   * @param von Nummer des zu untersuchenden Mitarbeiters
   * @param neu Nummer des potenziell neuen Chefs
   * @return wuerde durch Aenderung ein Zyklus entstehen?
   */
  private boolean istZyklus(int von, int neu) {
    if (von == neu) {
      return true;
    }
    var ma = this.find(neu);
    if (ma.get().getChef() != null) {
      return this.istZyklus(von, ma.get().getChef().getMinr());
    }
    return false;
  }

  /**
   * Aendert den Chef von Mitarbeiter mit Nummer von auf den Mitarbeiter mit der
   * Nummer neu. Dabei darf von so nicht direkt oder ueber Zwischenpersonen
   * Vorgesetzter von sich selbst werden. Weiterhin darf ein Chef maximal 3
   * Mitarbeiter betreuen.
   * 
   * @param von Nummer des Mitarbeiters mit neuem Chech
   * @param neu Nummer des neuen Chefs
   * @return gilt danach die gewuenschte Zuordnung?
   */
  public boolean neuerChefVon(int von, int neu) {
    var ma = this.find(von);
    var chef = this.find(neu);
    if (ma.isEmpty() || chef.isEmpty()) {
      return false;
    }
    if (ma.get().getChef() != null
        && ma.get().getChef().getMinr() == chef.get().getMinr()) {
      return true; // nichts geaendert
    }
    var untergeben = this.chefVon(neu);
    if (this.istZyklus(von, neu)) {
      throw new IllegalArgumentException(
          "niemand ist sein eigener Chef");
    }
    if (untergeben.size() > 2) {
      throw new IllegalArgumentException(
          neu + " hat zu viele Mitarbeiter");
    }
    try {
      if (ma.get().getChef() != null) {
        this.stmt.execute("UPDATE CHEFVON SET CHEFVON.Cminr ="
            + neu + " WHERE CHEFVON.Minr=" + von);
      } else {
        this.stmt.execute("INSERT INTO CHEFVON VALUES(" + von
            + "," + neu + ")");
      }
      this.con.commit();
      return true;
    } catch (SQLException ex) {
      System.err.println("SQL-Fehler: " + ex);
    }
    return false;
  }
}

package main;

import db.DAO;
import entity.Mitarbeiter;

public class Dialog {
  private DAO dao = new DAO();
  private EinUndAusgabe ea = new EinUndAusgabe();
  
  public void dialog(){
    this.dao.connect();
    int eingabe = -1;
    while (eingabe != 0) {
      System.out.println(
            "(0) Beenden\n"
          + "(1) Mitarbeiter hinzufuegen\n"
          + "(2) Mitarbeiter loeschen\n"
          + "(3) Chef von Mitarbeiter aendern: ");
      eingabe = this.ea.leseInteger();
      switch(eingabe) {
        case 1: {
          this.neuerMitarbeiter();
          break;
        }
        case 2: {
          this.mitarbeiterLoeschen();
          break;
        }
        case 3: {
          this.chefAendern();
          break;
        }
      }
      for( var m:this.dao.alle()) {
        System.out.println(m);
      }
    }
    this.dao.dbSchliessen();
  }

  private void neuerMitarbeiter() {
    System.out.print("Name: ");
    var name = this.ea.leseString();
    System.out.print("hat Chef (0) nein (1) ja: ");
    var hatchef = this.ea.leseInteger();
    Mitarbeiter chef = null;
    if (hatchef == 1) {
      System.out.print("Nummer des Chefs: ");
      var cnr = this.ea.leseInteger();
      var chefok = this.dao.find(cnr);
      if (chefok.isPresent()) {
        chef = chefok.get();
      } else {
        System.out.println("Chef nicht gefunden, hinzufuegen nicht erfolgreich");
      }
    }
    try {
      if (this.dao.mitarbeiterHinzu(new Mitarbeiter(name, chef))) {
        System.out.println("Hinzufuegen erfolgreich");
      } else {
        System.out.println("Hinzufuegen nicht erfolgreich");
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }
  
  private void mitarbeiterLoeschen() {
      System.out.print("Nummer: ");
      var nr = this.ea.leseInteger();
    try {
      if (this.dao.loeschen(nr)) {
        System.out.println("Loeschen erfolgreich");
      } else {
        System.out.println("Loeschen nicht erfolgreich");
      }
    } catch (Exception e) {
      System.out.println(e);
    }
  }  
  
  private void chefAendern() {
    System.out.print("Chef aendern von Nummer: ");
    var nr = this.ea.leseInteger();
    System.out.print("Nummer von neuem Chef: ");
    var cnr = this.ea.leseInteger();
  try {
    if (this.dao.neuerChefVon(nr, cnr)) {
      System.out.println("Aendern erfolgreich");
    } else {
      System.out.println("Aendern nicht erfolgreich");
    }
  } catch (Exception e) {
    System.out.println(e);
  }
}
}

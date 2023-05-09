package db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import entity.Mitarbeiter;

public class DAOTest {
  private DAO sut;
  private Mitarbeiter m1;
  private Mitarbeiter m2;
  private Mitarbeiter m3;
  private Mitarbeiter m4;
  private Mitarbeiter m5;

  @BeforeEach
  public void setUp() throws Exception {
    this.sut = new DAO();
    this.sut.connect();
    this.m1 = new Mitarbeiter("A", null);
    this.m2 = new Mitarbeiter("B", this.m1);
    this.m3 = new Mitarbeiter("C", this.m1);
    this.m4 = new Mitarbeiter("D", this.m1);
    this.m5 = new Mitarbeiter("E", this.m2);
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(m1));
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(m2));
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(m3));
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(m4));
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(m5));
  }
  
  @AfterEach
  public void tearDown() {
    this.sut.dbSchliessen();
    this.sut.resetAll();
  }

  @Test
  public void testNeuerMitarbeiterOK1() {
    Mitarbeiter ma = new Mitarbeiter("X", null);
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(ma));
    var find = this.sut.find(ma.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals("X", find.get().getName());
    Assertions.assertNull(find.get().getChef());
  }
  
  @Test
  public void testNeuerMitarbeiterOK2() {
    Mitarbeiter ma = new Mitarbeiter("X", this.m2);
    Assertions.assertTrue(this.sut.mitarbeiterHinzu(ma));
    var find = this.sut.find(ma.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals("X", find.get().getName());
    Assertions.assertEquals(this.m2, find.get().getChef());
  }

  @Test 
  public void testNeuerMitarbeiterChefAusgelastet() {
    Mitarbeiter ma = new Mitarbeiter("X", this.m1);
    Executable auszufuehren = () -> this.sut.mitarbeiterHinzu(ma);
    Assertions.assertThrows(IllegalArgumentException.class
                  , auszufuehren);
    var find = this.sut.find(ma.getMinr());
    Assertions.assertTrue(find.isEmpty());
  }
  
  @Test
  public void testLoeschenVorhandenerMitarbeiter() {
    var find = this.sut.find(this.m5.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m5.getName(), find.get().getName());
    Assertions.assertEquals(this.m5.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m5.getChef(), find.get().getChef());
    Assertions.assertTrue(this.sut.loeschen(this.m5.getMinr()));
    find = this.sut.find(this.m5.getMinr());
    Assertions.assertTrue(find.isEmpty());
    
    // Idempotent
    Assertions.assertTrue(this.sut.loeschen(this.m5.getMinr()));
    
    // andere unveraendert
    find = this.sut.find(this.m4.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m4.getName(), find.get().getName());
    Assertions.assertEquals(this.m4.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m4.getChef(), find.get().getChef());
  }
  
  @Test
  public void testLoeschenVorhandenerChef() {
    var find = this.sut.find(this.m1.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m1.getName(), find.get().getName());
    Assertions.assertEquals(this.m1.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m1.getChef(), find.get().getChef());
    Assertions.assertTrue(this.sut.loeschen(this.m1.getMinr()));
    find = this.sut.find(this.m1.getMinr());
    Assertions.assertTrue(find.isEmpty());
    
    // Idempotent
    Assertions.assertTrue(this.sut.loeschen(this.m1.getMinr()));
    
    // andere haben Chef verloren
    find = this.sut.find(this.m4.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m4.getName(), find.get().getName());
    Assertions.assertEquals(this.m4.getMinr(), find.get().getMinr());
    Assertions.assertNull(find.get().getChef());
  }
  
  @Test
  public void testLoeschenNichtVorhandenerMitarbeiterOK() {
    Mitarbeiter ma = new Mitarbeiter("X", this.m4);
    Assertions.assertTrue(this.sut.loeschen(ma.getMinr()));
    var find = this.sut.find(ma.getMinr());
    Assertions.assertTrue(find.isEmpty());
  }
  
  @Test
  public void testNeuerChefOkOhneChefVorher() {
    Mitarbeiter ma = new Mitarbeiter("X", null);
    this.sut.mitarbeiterHinzu(ma);
    Assertions.assertTrue(this.sut.neuerChefVon(this.m1.getMinr(), ma.getMinr()));
    var find = this.sut.find(this.m1.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m1.getName(), find.get().getName());
    Assertions.assertEquals(this.m1.getMinr(), find.get().getMinr());
    Assertions.assertEquals(ma, find.get().getChef());
  }
  
  @Test
  public void testNeuerChefOkMitChefVorher() {
    Assertions.assertTrue(this.sut.neuerChefVon(this.m5.getMinr(), this.m3.getMinr()));
    var find = this.sut.find(this.m5.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m5.getName(), find.get().getName());
    Assertions.assertEquals(this.m5.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m3, find.get().getChef());
  }
  
  @Test
  public void testNeuerChefOkGleicherChefWieVorher() {
    Assertions.assertTrue(this.sut.neuerChefVon(this.m2.getMinr(), this.m1.getMinr()));
    var find = this.sut.find(this.m2.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m2.getName(), find.get().getName());
    Assertions.assertEquals(this.m2.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m1, find.get().getChef());
  }
  
  @Test
  public void testNeuerChefNichtOkNichtDirektVonSichSelbst() {
    Executable auszufuehren = () -> this.sut.neuerChefVon(this.m1.getMinr(), this.m1.getMinr());
    Assertions.assertThrows(IllegalArgumentException.class
                  , auszufuehren);
    
    var find = this.sut.find(this.m1.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m1.getName(), find.get().getName());
    Assertions.assertEquals(this.m1.getMinr(), find.get().getMinr());
    Assertions.assertNull(this.m1.getChef());
  }
  
  @Test
  public void testNeuerChefNichtOkNichtInDirektVonSichSelbst() {
    Executable auszufuehren = () -> this.sut.neuerChefVon(this.m1.getMinr(), this.m5.getMinr());
    Assertions.assertThrows(IllegalArgumentException.class
                  , auszufuehren);
    
    var find = this.sut.find(this.m1.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m1.getName(), find.get().getName());
    Assertions.assertEquals(this.m1.getMinr(), find.get().getMinr());
    Assertions.assertNull(this.m1.getChef());
  }
  
  @Test
  public void testNeuerChefNichtOkNichtZuVieleMitarbeiter() {
    int alterChef = this.m5.getChef().getMinr();
    Executable auszufuehren = () -> this.sut.neuerChefVon(this.m5.getMinr(), this.m1.getMinr());
    Assertions.assertThrows(IllegalArgumentException.class
                  , auszufuehren);
    
    var find = this.sut.find(this.m5.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m5.getName(), find.get().getName());
    Assertions.assertEquals(this.m5.getMinr(), find.get().getMinr());
    Assertions.assertEquals(alterChef, this.m5.getChef().getMinr());    
  }
  
  @Test
  public void testNeuerChefNichtOkChefExistiertNicht() {
    Mitarbeiter ma = new Mitarbeiter("X", null);
    Assertions.assertFalse(this.sut.neuerChefVon(this.m5.getMinr(), ma.getMinr()));
    var find = this.sut.find(this.m5.getMinr());
    Assertions.assertFalse(find.isEmpty());
    Assertions.assertEquals(this.m5.getName(), find.get().getName());
    Assertions.assertEquals(this.m5.getMinr(), find.get().getMinr());
    Assertions.assertEquals(this.m2, find.get().getChef());
  }
  
  @Test
  public void testNeuerChefNichtOkMitarbeiterExistiertNicht() {
    Mitarbeiter ma = new Mitarbeiter("X", null);
    Assertions.assertFalse(this.sut.neuerChefVon(ma.getMinr(), this.m5.getMinr()));
  }
  
  @Test
  public void testAlleOK1() {
    var alle = this.sut.alle();
    Assertions.assertEquals( 5, alle.size());
    Assertions.assertTrue(alle.contains(this.m1));
    Assertions.assertTrue(alle.contains(this.m2));
    Assertions.assertTrue(alle.contains(this.m3));
    Assertions.assertTrue(alle.contains(this.m4));
    Assertions.assertTrue(alle.contains(this.m5));
  }
  
  @Test
  public void testAlleOK2() {
    this.sut.dbSchliessen();
    this.sut.resetAll();
    this.sut.connect();
    var alle = this.sut.alle();
    Assertions.assertEquals( 0, alle.size());
  }
}
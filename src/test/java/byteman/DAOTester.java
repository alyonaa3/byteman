package db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.byteman.contrib.bmunit.WithByteman;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import entity.Mitarbeiter;

@WithByteman
public class DAOTester {
	private DAO sut;
	private Mitarbeiter m1;
	private Mitarbeiter m2;
	private Mitarbeiter m3;
	private Mitarbeiter m4;
	private Mitarbeiter m5;
	private Mitarbeiter m6;


	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@BeforeEach
	public void setUp() throws Exception {
		this.sut = new DAO();
		this.sut.connect();
		this.m1 = new Mitarbeiter("A", null);
		this.m2 = new Mitarbeiter("B", this.m1);
		this.m3 = new Mitarbeiter("C", this.m1);
		this.m4 = new Mitarbeiter("D", this.m1);
		this.m5 = new Mitarbeiter("E", this.m2);
		this.m5 = new Mitarbeiter("D", this.m2);
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
		Assertions.assertThrows(IllegalArgumentException.class, auszufuehren);
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

	// EIGENER CODE
	@BMRule(name = "Custom SQL Exception Loeschen", 
			targetClass = "java.sql.Statement",
			isInterface = true, targetMethod = "execute",
			targetLocation = "EXIT", 
			condition = "callerEquals(\"DAO.loeschen\",true)",
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testLoeschenSQLException() {
		exception.expect(SQLException.class);
		this.sut.loeschen(this.m1.getMinr());
	}

	@BMRule(name = "Custom SQL Exception find",
			targetClass = "java.sql.Statement", 
			isInterface = true, 
			targetMethod = "executeQuery", 
			targetLocation = "EXIT",
			condition = "callerEquals(\"DAO.find\",true)", 
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testFindSQLException() {
		exception.expect(SQLException.class);
		this.sut.find(this.m1.getMinr());
	}

	@BMRule(name = "Custom SQL Exception neuerChefVon",
			targetClass = " java.sql.Connection",
			isInterface = true, targetMethod = "commit", 
			targetLocation = "EXIT",
			condition = "callerEquals(\"DAO.neuerChefVon\",true)", 
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testneuerChefVonSQLException() {
		exception.expect(SQLException.class);
		this.sut.neuerChefVon(this.m4.getMinr(), this.m2.getMinr());
	}

	@BMRule(name = "Custom SQL Exception Alle", 
			targetClass = "java.sql.Statement", 
			isInterface = true,
			targetMethod = "executeQuery",
			targetLocation = "EXIT",
			condition = "callerEquals(\"DAO.alle\",true)",
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testAlleSQLException() {
		exception.expect(SQLException.class);
		this.sut.alle();
	}

	@BMRule(name = "Custom SQL Exception Mit Hinzu",
			targetClass = "java.sql.Statement",
			isInterface = true,
			targetMethod = "execute",
			targetLocation = "ENTRY",
			condition = "callerEquals(\"DAO.mitarbeiterHinzu\",true)",
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testMitarbeiterHinzuSQLException() {
		exception.expect(SQLException.class);
		this.sut.mitarbeiterHinzu(m6);
	}
	

	@BMRule(name = "Custom SQL Exception Connection Close", 
			targetClass = "java.sql.Connection", 
			isInterface = true, targetMethod = "close",
			targetLocation = "EXIT", 
			condition = "callerEquals(\"DAO.dbSchliessen\",true)",
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testdbSchliessenSQLException() {
		exception.expect(SQLException.class);
		this.sut.dbSchliessen();
	}

	@Test
	public void testdbSchliessenConNULL()
			throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
		Field m = this.sut.getClass().getDeclaredField("con");
		m.setAccessible(true);
		m.set(this.sut, null);
		this.sut.dbSchliessen();
	}

	@Test
	public void testFileIsDir() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		new File(".\\test").mkdirs();
		new File(".\\test\\test2").mkdirs();
		File f = new File(".\\test");
		Method m = this.sut.getClass().getDeclaredMethod("deleteDir", File.class);
		m.setAccessible(true);
		m.invoke(this.sut, f);
	}

	@BMRule(name = "Custom SQL Exception chefVon", 
			targetClass = "java.sql.Statement", 
			isInterface = true, 
			targetMethod = "executeQuery", 
			targetLocation = "EXIT", 
			condition = "callerEquals(\"DAO.chefVon\",true)", 
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testchefVonSQLException() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Method m = this.sut.getClass().getDeclaredMethod("chefVon", int.class);
		m.setAccessible(true);
		exception.expect(SQLException.class);
		m.invoke(this.sut, this.m1.getMinr());
	}

	@BMRules(rules = {
			@BMRule(name = "Skript Geladen = false",
					targetClass = "db.DAO",
					targetMethod = "skriptLaden",
					targetLocation = "EXIT",
					condition = "callerEquals(\"DAO.connect\",true)",
					action = "RETURN FALSE"),
			@BMRule(name = "Custom SQL Exception connect",
					isInterface = true, 
					targetClass = "java.sql.Connection",
					targetMethod = "createStatement",
					targetLocation = "EXIT",
					condition = "callerEquals(\"DAO.connect\",true)",
					action = "System.out.println(\"connect Tettt\"); throw new SQLException()") })
	@Test
	public void testConnectSkriptGeladenFalse() {
		exception.expect(SQLException.class);

		this.sut.dbSchliessen();
		this.sut.resetAll();
		this.sut.connect();
	}
	
	@BMRule(name = "Custom SQL Exception connect EXCZ",
			targetClass = "java.sql.Connection",
			isInterface = true, 
			targetMethod = "createStatement", 
			targetLocation = "EXIT",
			condition = "TRUE",
			action = "System.out.println(\"connect Tetttt\"); throw new Exception()")
	@Test
	public void testConnectSQLEXceptziopn() {
		exception.expect(Exception.class);
		this.sut.resetAll();
		this.sut.dbSchliessen();
		this.sut.connect();
	}

	@BMRule(name = "Custom SQL Exception connect richtig geschrieben",
			targetClass = "java.sql.Statement",
			isInterface = true, 
			targetMethod = "executeQuery",
			targetLocation = "EXIT",
			condition = "callerEquals(\"DAO.connect\",true)",
			action = "System.out.println(\"connect Tettttt\"); throw new SQLException()")

	@Test
	public void testConnectSQLException() {
		this.sut.dbSchliessen();
		exception.expect(SQLException.class);

		this.sut.connect();
	}

	@BMRule(name = "Custom SQL Exception Berechne Tabellen",
			targetClass = " java.sql.Connection",
			isInterface = true, targetMethod = "getMetaData",
			targetLocation = "EXIT",
			condition = "callerEquals(\"DAO.berechneTabellen\",true)",
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testBerechneTabellenSQLException() throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Method m = this.sut.getClass().getDeclaredMethod("berechneTabellen");
		m.setAccessible(true);
		exception.expect(SQLException.class);
		m.invoke(this.sut);
	}

	@BMRule(name = "Custom SQL Exception Script Laden", 
			targetClass = " org.h2.tools.RunScript",
			isInterface = true, 
			targetMethod = "execute", 
			targetLocation = "EXIT", 
			condition = "callerEquals(\"DAO.skriptLaden\",true)", 
			action = "System.out.println(\"testname\"); throw new SQLException()")
	@Test
	public void testScriptLadenSQLException() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {
		Method m = this.sut.getClass().getDeclaredMethod("skriptLaden", String.class);
		m.setAccessible(true);
		exception.expect(SQLException.class);
		m.invoke(this.sut, "lol");
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
		Assertions.assertThrows(IllegalArgumentException.class, auszufuehren);

		var find = this.sut.find(this.m1.getMinr());
		Assertions.assertFalse(find.isEmpty());
		Assertions.assertEquals(this.m1.getName(), find.get().getName());
		Assertions.assertEquals(this.m1.getMinr(), find.get().getMinr());
		Assertions.assertNull(this.m1.getChef());
	}

	@Test
	public void testNeuerChefNichtOkNichtInDirektVonSichSelbst() {
		Executable auszufuehren = () -> this.sut.neuerChefVon(this.m1.getMinr(), this.m5.getMinr());
		Assertions.assertThrows(IllegalArgumentException.class, auszufuehren);

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
		Assertions.assertThrows(IllegalArgumentException.class, auszufuehren);

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
		Assertions.assertEquals(5, alle.size());
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
		Assertions.assertEquals(0, alle.size());
	}
}
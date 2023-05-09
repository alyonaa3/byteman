package main;


//Aufruf der Tests mit -javaagent:lib\byteman.jar=script:regeln.btm
public class Main {
  public static void main(String[] s) {
    new Dialog().dialog();
  }
}

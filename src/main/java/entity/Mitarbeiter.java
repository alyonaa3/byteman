package entity;

public class Mitarbeiter {
  private int minr;
  private String name;
  private Mitarbeiter chef;
  private static int micount =1000;

  public Mitarbeiter(String name, Mitarbeiter chef) {
    super();
    this.name = name;
    this.chef = chef;
    this.minr = micount++;
  }
  
  public static void setMicount(int val) {
    micount = val;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
  
  public Mitarbeiter getChef() {
    return chef;
  }
  
  public void setChef(Mitarbeiter chef) {
    this.chef = chef;
  }
  
  public int getMinr() {
    return minr;
  }
  
  public void setMinr(int nr) {
    this.minr = nr;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + minr;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Mitarbeiter other = (Mitarbeiter) obj;
    if (minr != other.minr)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Mitarbeiter [minr=" + minr + ", name=" + name + ", chef="
        + (chef!=null ? ""+ chef.minr: "NULL") + "]";
  }
  
  
}

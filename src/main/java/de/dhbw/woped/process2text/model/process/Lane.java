package de.dhbw.woped.process2text.model.process;

public class Lane {
  private final String name;
  private static final String EXCEPTION_STRING = "All";
  private String bpmnid;

  public Lane(String name, String pool) {
    if (pool.equals(EXCEPTION_STRING)) {
      this.name = name;
    } else {
      this.name = name + " from " + pool;
    }
  }

  public void addBPMNId(String id) {
    this.bpmnid = id;
  }

  public String getBPMNId() {
    return bpmnid;
  }

  public String getName() {
    return name;
  }
}

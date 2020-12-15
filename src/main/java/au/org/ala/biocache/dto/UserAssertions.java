package au.org.ala.biocache.dto;

import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class UserAssertions {

  List<QualityAssertion> userAssertions = new ArrayList();

  public UserAssertions() {}

  public List<QualityAssertion> getUserAssertions() {
    return userAssertions;
  }

  public void addUserAssertion(QualityAssertion qa) {
    userAssertions.add(qa);
  }

  public void deleteUserAssertion(String uuid) {
    for (int i=0;i<userAssertions.size();i++) {
      if (userAssertions.get(i).getUuid().equals(uuid)) {
        userAssertions.remove(i);
      }
    }
  }
}
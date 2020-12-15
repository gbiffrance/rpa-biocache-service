package au.org.ala.biocache.dao;

import au.org.ala.biocache.dto.QualityAssertion;

import java.io.IOException;

/**
 * Permanent data store interface.
 */
public interface StoreDAO {
  <T> T get(Class<T> dataClass, String key) throws IOException;

  <T> void put(String key, T data) throws IOException;

  <T> Boolean delete(Class<T> dataClass, String key) throws IOException;
}

package au.org.ala.biocache.dao;

import au.org.ala.biocache.dto.IndexFieldDTO;
import au.org.ala.biocache.dto.SearchRequestParams;
import au.org.ala.biocache.dto.SpatialSearchRequestParams;
import au.org.ala.biocache.dto.StatsIndexFieldDTO;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;

import java.util.Map;
import java.util.Set;

/**
 * Index interface for queries.
 */
public interface IndexDAO {
    void init();

    void destroy();

    QueryResponse query(SolrParams query, Double version) throws Exception;

    Long getIndexVersion(Boolean force);

    /**
     * Returns the up to date statistics for the supplied field
     * @param field
     * @return
     * @throws Exception
     */
    Set<IndexFieldDTO> getIndexFieldDetails(String... field) throws Exception;

    StatsIndexFieldDTO getRangeFieldDetails(String field);

    Set<IndexFieldDTO> getIndexedFields() throws Exception;

    Set<IndexFieldDTO> getIndexedFields(boolean update) throws Exception;

    Map<String, IndexFieldDTO> getIndexedFieldsMap() throws Exception;

    Map<String, FieldStatsInfo> getStatistics(SpatialSearchRequestParams searchParams) throws Exception;

    QueryResponse runSolrQuery(SolrQuery solrQuery, SearchRequestParams requestParams) throws Exception;

    Map<String, String> getNewToLegacy();
}

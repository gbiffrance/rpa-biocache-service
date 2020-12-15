package au.org.ala.biocache.dto;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.ArrayList;
import java.util.List;

/**
 * Merged from biocache-store
 */
public class DuplicateRecordDetails {
    String id;
    Integer precision;
    String status;
    String duplicateOf;
    List<DuplicateRecordDetails> duplicates;
    List<String> dupTypes;

    public static final String DUPLICATE_OF = "duplicate_of";
    public static final String DUPLICATE_STATUS = "duplicate_status";
    public static final String DUPLICATE_REASONS = "duplicate_reasons";
    public static final String ID = "ID";


    public DuplicateRecordDetails() {};

    public DuplicateRecordDetails(SolrDocument d) {
        this.id = (String) d.getFieldValue(ID);
        this.status = (String) d.getFieldValue(DUPLICATE_STATUS);

        if ("D".equals(status)) {
            // is duplicate
            this.dupTypes = new ArrayList();
            for (Object dupType : d.getFieldValues(DUPLICATE_REASONS)) {
                dupTypes.add(dupType.toString());
            }
            this.duplicateOf = (String) d.getFieldValue(DUPLICATE_OF);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDuplicateOf() {
        return duplicateOf;
    }

    public void setDuplicateOf(String duplicateOf) {
        this.duplicateOf = duplicateOf;
    }

    public List<DuplicateRecordDetails> getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(List<DuplicateRecordDetails> duplicates) {
        this.duplicates = duplicates;
    }

    public List<String> getDupTypes() {
        return dupTypes;
    }

    public void setDupTypes(List<String> dupTypes) {
        this.dupTypes = dupTypes;
    }
}

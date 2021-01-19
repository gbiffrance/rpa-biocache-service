/**************************************************************************
 *  Copyright (C) 2010 Atlas of Living Australia
 *  All Rights Reserved.
 * 
 *  The contents of this file are subject to the Mozilla Public
 *  License Version 1.1 (the "License"); you may not use this file
 *  except in compliance with the License. You may obtain a copy of
 *  the License at http://www.mozilla.org/MPL/
 * 
 *  Software distributed under the License is distributed on an "AS
 *  IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.dto;

import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * A DTO representing an result from the search indexes.
 */
public interface OccurrenceIndex {

    // SOLR fields that used in biocache-service and have the same name and type in all schema versions
    final static public String ROW_KEY = "id";
    final static public String IMAGES = "images";
    final static public String SOUNDS = "sounds";
    final static public String GEOSPATIAL_KOSHER = "geospatial_kosher";
    final static public String COUNTRY = "country";
    final static public String STATE = "state";
    final static public String PROVENANCE = "provenance";

    public static final String OCCURRENCE_YEAR_INDEX_FIELD = "occurrence_year";

    //sensitive fields and their non-sensitive replacements
    public static final String[] sensitiveSOLRHdr = {"sensitive_longitude", "sensitive_latitude", "sensitive_locality", "sensitive_event_date", "sensitive_event_date_end", "sensitive_grid_reference"};
    public static final String[] notSensitiveSOLRHdr10 = {OccurrenceIndex10.LONGITUDE, OccurrenceIndex10.LATITUDE, OccurrenceIndex10.LOCALITY};
    public static final String[] notSensitiveSOLRHdr20 = {OccurrenceIndex20.LONGITUDE, OccurrenceIndex20.LATITUDE, OccurrenceIndex20.LOCALITY};
    public static final String CONTAINS_SENSITIVE_PATTERN = StringUtils.join(sensitiveSOLRHdr, "|");

    public static final String NAMES_AND_LSID = "names_and_lsid";
    public static final String COMMON_NAME_AND_LSID = "common_name_and_lsid";
    public static final String DECADE_FACET_NAME = "decade";

    public static final String spatialField = "geohash";

    public static final String DUPLICATE_OF = "duplicate_of";
    public static final String DUPLICATE_STATUS = "duplicate_status";
    public static final String DUPLICATE_REASONS = "duplicate_reasons";
    public static final String ID = "id";

    public static final String SPECIES_SUBGROUP = "species_subgroup";
    public static final String SPECIES_GROUP = "species_group";
    public static final String IMAGE_URL = "image_url";
    public static final String LAT_LNG = "lat_long";
    public static final String ALL_IMAGE_URL = "all_image_url";
    public static final String RAW_NAME = "raw_name";
    public static final String LFT = "lft";
    public static final String MONTH = "month";
    public static final String ASSERTIONS = "assertions";
    String SUBSPECIES_NAME = "subspecies_name";
    String SPECIES = "species";
    String GENUS = "genus";
    String FAMILY = "family";
    String ORDER = "order";
    String CLASS = "class";
    String PHYLUM = "phylum";
    String KINGDOM = "kingdom";
    String INTERACTION = "interaction";
    String SENSITIVE = "sensitive";
    String RGT = "rgt";

    Double getDecimalLongitude();

    Double getDecimalLatitude();

    String getUuid();

    void setThumbnailUrl(String thumb);

    void setLargeImageUrl(String large);

    void setSmallImageUrl(String small);

    void setImageUrl(String raw);

    String getImage();

    String[] getImages();

    void setImageUrls(String[] imageUrls);

    void setImageMetadata(List<Map<String, Object>> imageMetadata);
}

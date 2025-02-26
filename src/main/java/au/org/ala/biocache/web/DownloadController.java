/**************************************************************************
 * Copyright (C) 2013 Atlas of Living Australia
 * All Rights Reserved.
 * <p/>
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 * <p/>
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.biocache.web;

import au.org.ala.biocache.dao.PersistentQueueDAO;
import au.org.ala.biocache.dao.SearchDAO;
import au.org.ala.biocache.dto.DownloadDetailsDTO;
import au.org.ala.biocache.dto.DownloadRequestParams;
import au.org.ala.biocache.service.AuthService;
import au.org.ala.biocache.service.DownloadService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.PropertyFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

/**
 * A Controller for downloading records based on queries.  This controller
 * will provide methods for offline asynchronous downloads of large result sets.
 * <ul> 
 * <li> persistent queue to contain the offline downloads. - written to filesystem before emailing to supplied user </li>
 * <li> administering the queue - changing order, removing items from queue </li>
 * </ul> 
 * @author Natasha Carter (natasha.carter@csiro.au)
 */
@Controller
public class DownloadController extends AbstractSecureController {

    /** Fulltext search DAO */
    @Inject
    protected SearchDAO searchDAO;

    @Inject
    protected PersistentQueueDAO persistentQueueDAO;

    @Inject
    protected AuthService authService;

    @Inject
    protected DownloadService downloadService;

    @Value("${download.auth.bypass:false}")
    boolean authBypass = false;

    @Value("${download.auth.role:ROLE_USER}")
    String downloadRole;

    /**
     * Retrieves all the downloads that are on the queue
     * @return
     */
    @RequestMapping(value = { "occurrences/offline/download/stats", "occurrences/offline/download/stats.json" }, method = RequestMethod.GET)
    public @ResponseBody
    List getCurrentDownloads(
            HttpServletResponse response,
            @RequestParam(value = "apiKey", required = true) String apiKey) throws Exception {
        if (apiKey != null) {
            if (shouldPerformOperation(apiKey, response, false)) {
                JsonConfig config = new JsonConfig();
                config.setJsonPropertyFilter(new PropertyFilter() {
                    @Override
                    public boolean apply(Object source, String name, Object value) {
                        return value == null;
                    }
                });

                JSONArray ja = JSONArray.fromObject(persistentQueueDAO.getAllDownloads(), config);
                for (Object jo : ja) {
                    String id = (String) ((net.sf.json.JSONObject) jo).get("uniqueId");
                    ((net.sf.json.JSONObject) jo).put("cancelURL", downloadService.webservicesRoot + "/occurrences/offline/cancel/" + id + "?apiKey=" + apiKey);
                }
                return ja;
            }
        }
        return null;
    }

    /**
     * Add a download to the offline queue
     * @param requestParams
     * @param ip
     * @param apiKey
     * @param type
     * @param response
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = { "occurrences/offline/{type}/download*", "occurrences/offline/{type}/download.json*" } , method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody Object occurrenceDownload(
            DownloadRequestParams requestParams,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "email", required = true) String email,
            @PathVariable("type") String type,
            HttpServletResponse response,
            HttpServletRequest request) throws Exception {

        return download(requestParams, ip, getUserAgent(request), apiKey, email, response, request, DownloadDetailsDTO.DownloadType.RECORDS_INDEX);
    }

    /**
     * Add a download to the offline queue
     * @param requestParams
     * @param ip
     * @param apiKey
     * @param response
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping(value = { "occurrences/offline/download*", "occurrences/offline/download.json*" }, method = {RequestMethod.GET, RequestMethod.POST})
    public @ResponseBody Object occurrenceDownload(
            DownloadRequestParams requestParams,
            @RequestParam(value = "ip", required = false) String ip,
            @RequestParam(value = "apiKey", required = false) String apiKey,
            @RequestParam(value = "email", required = true) String email,
            HttpServletResponse response,
            HttpServletRequest request) throws Exception {

        return download(requestParams, ip, getUserAgent(request), apiKey, email, response, request, DownloadDetailsDTO.DownloadType.RECORDS_INDEX);

    }

    private Object download(DownloadRequestParams requestParams, String ip, String userAgent, String apiKey, String email,
                            HttpServletResponse response, HttpServletRequest request,
                            DownloadDetailsDTO.DownloadType downloadType) throws Exception {

        // check the email is supplied and a matching user account exists with the required privileges
        if (StringUtils.isEmpty(email)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED, "Unable to perform an offline download without an email address");
            return null;

        }

        if (!authBypass) {

            // lookup the user details and check privileges based on the supplied email
            try {
                Map<String, Object> userDetails = (Map<String, Object>) authService.getUserDetails(email);

                if (userDetails == null) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unable to perform an offline download, user not recognised");
                    return null;
                }

                boolean activated = (Boolean) userDetails.getOrDefault("activated", true);
                boolean locked = (Boolean) userDetails.getOrDefault("locked", true);
                boolean hasRole = false;

                if (downloadRole == null) {
                    // no download role defined, allow based on role privileges
                    hasRole = true;
                } else {
                    // check that the user roles contains the download role
                    List<String> roles = (List<String>) userDetails.get("roles");
                    hasRole = roles == null ? false : roles.stream().anyMatch(downloadRole::equals);
                }

                if (!activated || locked || !hasRole) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unable to perform an offline download, insufficient privileges");
                    return null;
                }
            } catch (Exception e) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unable to perform an offline download, unable to verify user details");
                return null;
            }
        }

        boolean includeSensitive = false;
        if (apiKey != null) {
            if (shouldPerformOperation(apiKey, response, false)) {
                includeSensitive = true;
            }
        }

        // Pre SDS roles the sensitive flag controlled access to sensitive data.
        // After SDS roles were introduced, sensitiveFq variable drives the logic for sensitive data down the excution flow.
        // In Summary either sensitive is true or sensitiveFq is not null but not both

        //get the fq that includes only the sensitive data that the userId ROLES permits
        String sensitiveFq = null;
        if (!includeSensitive) {
            sensitiveFq = getSensitiveFq(request);
        }

        ip = ip == null ? request.getRemoteAddr() : ip;

        //create a new task
        DownloadDetailsDTO dd = new DownloadDetailsDTO(requestParams, ip, userAgent, downloadType);
        dd.setIncludeSensitive(includeSensitive);
        dd.setSensitiveFq(sensitiveFq);

        //get query (max) count for queue priority
        requestParams.setPageSize(0);
        requestParams.setFacet(false);
        SolrDocumentList result = searchDAO.findByFulltext(requestParams);
        dd.setTotalRecords(result.getNumFound());

        Map<String, Object> status = new LinkedHashMap<>();
        DownloadDetailsDTO d = persistentQueueDAO.isInQueue(dd);

        if (d != null) {
            status.put("message", "Already in queue.");
            status.put("status", "inQueue");
            status.put("queueSize", persistentQueueDAO.getTotalDownloads());
            status.put("statusUrl", downloadService.webservicesRoot + "/occurrences/offline/status/" + d.getUniqueId());
        } else if (dd.getTotalRecords() > downloadService.dowloadOfflineMaxSize) {
            //identify this download as too large
            File file = new File(downloadService.biocacheDownloadDir + File.separator + UUID.nameUUIDFromBytes(dd.getEmail().getBytes(StandardCharsets.UTF_8)) + File.separator + dd.getStartTime() + File.separator + "tooLarge");
            FileUtils.forceMkdir(file.getParentFile());
            FileUtils.writeStringToFile(file, "", "UTF-8");
            status.put("downloadUrl", downloadService.biocacheDownloadUrl);
            status.put("status", "skipped");
            status.put("message", downloadService.downloadOfflineMsg);
            status.put("error", "Requested to many records (" + dd.getTotalRecords() + "). The maximum is (" + downloadService.dowloadOfflineMaxSize + ")");
        } else {
            persistentQueueDAO.addDownloadToQueue(dd);
            status.put("status", "inQueue");
            status.put("queueSize", persistentQueueDAO.getTotalDownloads());
            status.put("statusUrl", downloadService.webservicesRoot + "/occurrences/offline/status/" + dd.getUniqueId());
        }

        status.put("searchUrl", downloadService.generateSearchUrl(dd.getRequestParams()));
        writeStatusFile(dd.getUniqueId(), status);

        return status;
    }

    @RequestMapping(value = { "occurrences/offline/status", "occurrences/offline/status.json" }, method = RequestMethod.GET)
    public @ResponseBody Object allOccurrenceDownloadStatus() throws Exception {

        List<Map<String, Object>> allStatus = new ArrayList<Map<String, Object>>();

        Map<String,String> userIdLookup = authService.getMapOfEmailToId();
        //is it in the queue?
        List<DownloadDetailsDTO> downloads = persistentQueueDAO.getAllDownloads();
        for (DownloadDetailsDTO dd : downloads) {
            Map<String, Object> status = new LinkedHashMap<>();
            String id = dd.getUniqueId();
            if (dd.getFileLocation() == null) {
                status.put("status", "inQueue");
            } else {
                status.put("status", "running");
                status.put("records", dd.getRecordsDownloaded());
            }
            status.put("id", id);
            status.put("totalRecords", dd.getTotalRecords());
            status.put("downloadParams", dd.getDownloadParams());
            status.put("startDate", dd.getStartDateString());
            status.put("thread", dd.getProcessingThreadName());
            if (userIdLookup != null) {
                status.put("userId", authService.getMapOfEmailToId().get(dd.getEmail()));
            }
            status.put("statusUrl", downloadService.webservicesRoot + "/occurrences/offline/status/" + id);

            setStatusIfEmpty(id, status);

            allStatus.add(status);
        }

        return allStatus;
    }

    private void setStatusIfEmpty(String id, Map<String, Object> status) throws UnsupportedEncodingException {
        //is it finished?
        if (!status.containsKey("status")) {
            int sep = id.lastIndexOf('-');
            File dir = new File(downloadService.biocacheDownloadDir + File.separator + id.substring(0, sep) + File.separator + id.substring(sep + 1));
            if (dir.isDirectory() && dir.exists()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getPath().endsWith(".zip") && file.length() > 0) {
                        status.put("status", "finished");
                        status.put("downloadUrl", downloadService.biocacheDownloadUrl + File.separator + URLEncoder.encode(file.getPath().replace(downloadService.biocacheDownloadDir + "/", ""), "UTF-8").replace("%2F", "/").replace("+", "%20"));
                    }
                    if (file.isFile() && "tooLarge".equals(file.getName())) {
                        status.put("status", "skipped");
                        status.put("message", downloadService.downloadOfflineMsg);
                        status.put("downloadUrl", downloadService.dowloadOfflineMaxUrl);
                    }
                }
                if (!status.containsKey("status")) {
                    status.put("status", "failed");
                }
            }
        }

        if (!status.containsKey("status")) {
            status.put("status", "invalidId");
        }
    }

    private void writeStatusFile(String id, Map status) throws IOException {
        File statusDir = new File(downloadService.biocacheDownloadDir + "/" + id.replaceAll("-([0-9]*)$", "/$1"));
        statusDir.mkdirs();
        String json = net.sf.json.JSONObject.fromObject(status).toString();
        FileUtils.writeStringToFile(new File(statusDir.getPath() + "/status.json"), json, "UTF-8");
    }

    @RequestMapping(value = { "occurrences/offline/status/{id}", "occurrences/offline/status/{id}.json" }, method = RequestMethod.GET)
    public @ResponseBody Object occurrenceDownloadStatus(@PathVariable("id") String id) throws Exception {

        Map<String, Object> status = new LinkedHashMap<>();

        //is it in the queue?
        List<DownloadDetailsDTO> downloads = persistentQueueDAO.getAllDownloads();
        for (DownloadDetailsDTO dd : downloads) {
            if (id.equals(dd.getUniqueId())) {
                if (dd.getFileLocation() == null) {
                    status.put("status", "inQueue");
                } else {
                    status.put("status", "running");
                    status.put("records", dd.getRecordsDownloaded());
                }
                status.put("totalRecords", dd.getTotalRecords());
                status.put("statusUrl", downloadService.webservicesRoot + "/occurrences/offline/status/" + id);
                if (authService.getMapOfEmailToId() != null) {
                    status.put("userId", authService.getMapOfEmailToId().get(dd.getEmail()));
                }
                break;
            }
        }

        //is it finished?
        String cleanId = id.replaceAll("[^a-z\\-0-9]", "");
        cleanId = cleanId.replaceAll("-([0-9]*)$", "/$1");
        if (!status.containsKey("status")) {
            File dir = new File(downloadService.biocacheDownloadDir + File.separator + cleanId);
            if (dir.isDirectory() && dir.exists()) {
                for (File file : dir.listFiles()) {
                    if (file.isFile() && file.getPath().endsWith(".zip") && file.length() > 0) {
                        status.put("status", "finished");
                        status.put("downloadUrl", downloadService.biocacheDownloadUrl + File.separator + URLEncoder.encode(file.getPath().replace(downloadService.biocacheDownloadDir + "/", ""), "UTF-8").replace("%2F", "/").replace("+", "%20"));
                    }
                    if (file.isFile() && "tooLarge".equals(file.getName())) {
                        status.put("status", "skipped");
                        status.put("message", downloadService.downloadOfflineMsg);
                        status.put("downloadUrl", downloadService.dowloadOfflineMaxUrl);
                        status.put("error", "requested to many records. The upper limit is (" + downloadService.dowloadOfflineMaxSize + ")");
                    }
                }
                if (!status.containsKey("status")) {
                    status.put("status", "failed");
                }
            }

            // write final status to a file
            if (status.containsKey("status")) {
                writeStatusFile(cleanId, status);
            }
        }

        if (!status.containsKey("status")) {
            //check downloads directory for a status file
            File file = new File(downloadService.biocacheDownloadDir + File.separator + cleanId + "/status.json");
            if (file.exists()) {
                status.putAll(JSONObject.fromObject(FileUtils.readFileToString(file, "UTF-8")));

                // the status.json is only used when a download request is 'lost'. Use an appropriate status.
                status.put("status", "unavailable");
                status.put("message", "This download is unavailable.");
            }
        }

        if (!status.containsKey("status")) {
            status.put("status", "invalidId");
        }
        return status;
    }

    /**
     * Cancel queued download. Does not cancel a download in progress.
     *
     * @param id
     * @return
     * @throws Exception
     */
    @RequestMapping(value = { "occurrences/offline/cancel/{id}", "occurrences/offline/cancel/{id}.json" }, method = RequestMethod.GET)
    public @ResponseBody Object occurrenceDownloadCancel(
            @PathVariable("id") String id,
            HttpServletResponse response,
            @RequestParam(value = "apiKey", required = true) String apiKey) throws Exception {

        if (apiKey == null || !shouldPerformOperation(apiKey, response, false)) {
            return null;
        }

        Map<String, Object> status = new LinkedHashMap<>();

        //is it in the queue?
        List<DownloadDetailsDTO> downloads = persistentQueueDAO.getAllDownloads();
        for (DownloadDetailsDTO dd : downloads) {
            if (id.equals(dd.getUniqueId())) {
                persistentQueueDAO.removeDownloadFromQueue(dd);
                status.put("cancelled", "true");
                status.put("status", "notInQueue");
                break;
            }
        }

        if (!status.containsKey("status")) {
            status.put("cancelled", "false");
            status.put("status", "notInQueue");
        }

        return status;
    }

    /**
     * IMPORTANT: Must return null if the user does not have access to sensitive data.
     *
     * @param request The request to get the "X-ALA-userId" header from to send to the authentication service to see if the user has access to sensitive data
     * @return Null if the user does not have access to sensitive data, and a Solr query string containing the filters giving the user access to sensitive data based on their assigned role otherwise
     * @throws ParseException If the sensitiveAccessRoles configuration was not parseable
     */
    private String getSensitiveFq(HttpServletRequest request) {
        if (!isValidKey(request.getHeader("apiKey"))) {
            return null;
        } else {
            String xAlaUserIdHeader = request.getHeader("X-ALA-userId");
            if (xAlaUserIdHeader == null) {
                return null;
            }
            return downloadService.getSensitiveFq(xAlaUserIdHeader);
        }
    }
}

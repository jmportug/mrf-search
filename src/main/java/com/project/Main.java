package com.project;

import com.google.gson.Gson;

import java.io.*;
import java.util.HashSet;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/*
TODO: look up by HIOS (maybe... see notes below)

The methodology is to get the eins from the file and then use the search by ein url to get the list of mrf urls.
I do this because the display names in the search by ein url seem to include the state code, which makes it easier to find the NY files
I do think there is room for improvement here, but I would need to do more research

For example:
It looks like "39B0" or "39F0" is in all of the display names for NY in-network returned by the ein lookup request
I haven't been able to check all of the name to be sure this pattern holds true
If this pattern is consistent, then we might be able to search for "39B0" in the urls of the index file
instead of getting the set of eins and using them to look up in the search by ein url
This also might allow us to avoid doing a look up by HIOS, but I need to confirm that this pattern holds true
for records that are have plan_type_id of HIOS

 */
public class Main {
    private final static String INDEX_FILE_URL = "https://antm-pt-prod-dataz-nogbd-nophi-us-east1.s3.amazonaws.com/anthem/2024-08-01_anthem_index.json.gz";
    // got the search by ein url by find the js function that does the ein lookup and
    // adding a console.log to print out the url
    private final static String SEARCH_BY_EIN_URL_FORMAT = "https://antm-pt-prod-dataz-nogbd-nophi-us-east1.s3.amazonaws.com/anthem/%s.json";
    private final static Gson gson = new Gson();


    // TODO: take in an optional arg that sets the number of eins to load.  a null value would mean do not limit the number of eins
    public static void main(String[] args) throws Exception {
        // setting limit 100 for the sake of testing/demo
        final Set<String> einSet = getEinSet(100);

        final Set<String> mrfUrls = getMrfUrlsFromEinSet(einSet);
        try(PrintWriter mrfFile = new PrintWriter("mrfFile.txt")) {
            for (String mrfUrl : mrfUrls) {
                mrfFile.println(mrfUrl);
            }
        }
        // TODO: load the mrfs from this mrfUrl set
    }

    // TODO: handle exception instead of just throwing it.  also throw the actual exception instead of a generic "Exception"
    private static Set<String> getMrfUrlsFromEinSet(Set<String> einSet) throws Exception {
        final Set<String> urls = new HashSet<>();
        int count =0;
        for (String ein : einSet) {
            count++;
            final URL url = new URL(String.format(SEARCH_BY_EIN_URL_FORMAT, ein));
            try (final Reader decoder = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                JsonObject einSearchResponse = gson.fromJson(decoder, JsonObject.class);
                // TODO: use an enum
                JsonArray files = einSearchResponse.get("In-Network Negotiated Rates Files").getAsJsonArray();
                for (JsonElement file : files) {
                    final JsonObject fileObject = file.getAsJsonObject();
                    final String displayName = fileObject.get("displayname").getAsString();
                    if (displayName.startsWith("NY_") || displayName.endsWith("_NY") || displayName.contains("_NY_")) {
                        final String fileUrl = fileObject.get("url").getAsString();
                        // System.out.println(displayName);
                        urls.add(fileUrl);
                    }
                }
            }
        }

        return urls;
    }

    // TODO: handle exception instead of just throwing it.  also throw the actual exception instead of a generic "Exception"
    private static Set<String> getEinSet(int fetchSize) throws Exception {
        // Sources on stream the file:
        // https://stackoverflow.com/questions/1080381/gzipinputstream-reading-line-by-line
        // https://stackoverflow.com/questions/6259339/how-to-read-a-text-file-directly-from-internet-using-java
        final URL url = new URL(INDEX_FILE_URL);
        final InputStream gzipStream = new GZIPInputStream(url.openStream());
        final Reader decoder = new InputStreamReader(gzipStream, StandardCharsets.UTF_8);

        final Set<String> einSet = new HashSet<>();
        try (final BufferedReader bufferedReader = new BufferedReader(decoder)) {
            String line;

            while ((line = bufferedReader.readLine()) != null && einSet.size() <= fetchSize ) {
                if (line.contains(IndexFileKey.REPORTING_PLANS.getKeyName())) {
                    // strip trailing comma
                    String reportingPlansJson = line.substring(0, line.length() - 1);

                    JsonObject reportingPlans = gson.fromJson(reportingPlansJson, JsonObject.class);
                    JsonArray reportingPlansArray = reportingPlans.getAsJsonArray(IndexFileKey.REPORTING_PLANS.getKeyName());

                    for (JsonElement reportingPlanElement : reportingPlansArray) {
                        JsonObject reportingPlan = reportingPlanElement.getAsJsonObject();
                        String planTypeId = reportingPlan.get(IndexFileKey.PLAN_ID_TYPE.getKeyName()).getAsString();
                        // other id type is HIOS
                        if (planTypeId.equals("EIN")) {
                            String planId = reportingPlan.get(IndexFileKey.PLAN_ID.getKeyName()).getAsString();
                            einSet.add(planId);

                        }
                    }
                }
            }


        }

        return einSet;
    }
}



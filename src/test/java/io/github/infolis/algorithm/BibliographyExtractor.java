package io.github.infolis.algorithm;

import io.github.infolis.InfolisBaseTest;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.Test;

/**
 *
 * @author domi
 */
public class BibliographyExtractor extends InfolisBaseTest {

    @Test
    public void test() {

        String reference = "Udvarhelyi, I.S., Gatsonis, C.A., Epstein, A.M., Pashos, C.L., Newhouse, J.P. and McNeil, B.J. Acute Myocardial Infarction in the Medicare population: process of care and clinical outcomes. Journal of the American Medical Association, 1992; 18:2530-2536. ";

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "curl",
                    "-H",
                    "\"Accept: application/json\"",
                    "-d",
                    "\"citation=" + reference + "\"",
                    "http://freecite.library.brown.edu/citations/create");

            System.out.println(pb.command());

            Process p = pb.start();
            InputStream is = p.getInputStream();
            
            JsonReader reader = Json.createReader(is);
            JsonArray o = reader.readArray();
            for(JsonObject single : o.getValuesAs(JsonObject.class)) {
                System.out.println(single.getJsonString("title"));
                System.out.println(single.getJsonString("journal"));
                //System.out.println(single.getJsonString("number"));
                //System.out.println(single.getJsonString("institution"));
                //System.out.println(single.getJsonString("publisher"));
                //System.out.println(single.getJsonString("editor"));
                System.out.println(single.getJsonNumber("year"));
                //System.out.println(single.getJsonString("note"));
                System.out.println(single.getJsonString("volume"));
                System.out.println(single.getJsonString("pages"));
                //System.out.println(single.getJsonString("booktitle"));
                System.out.println(single.getJsonArray("authors").get(0));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

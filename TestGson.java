import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.*;

public class TestGson {
    public static void main(String[] args) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = new JsonObject();
        json.addProperty("Line 1", "﷽ testing 123 😊");

        System.out.println("Direct JSON: " + json.toString());

        File f = new File("test_unicode.json");
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(f),
                java.nio.charset.StandardCharsets.UTF_8)) {
            gson.toJson(json, writer);
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(f),
                java.nio.charset.StandardCharsets.UTF_8)) {
            JsonObject readJson = gson.fromJson(reader, JsonObject.class);
            System.out.println("Read value: " + readJson.get("Line 1").getAsString());
        }
    }
}

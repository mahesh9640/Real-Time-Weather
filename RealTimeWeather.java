import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

public class WeatherMonitor {
    private static final String API_KEY = "your_openweathermap_api_key"; // Replace with your API key
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather?q=";
    private static final long FETCH_INTERVAL = 300000;  // 5 minutes in milliseconds
    private static final String[] CITIES = {"Mumbai", "Delhi", "Chennai", "Kolkata", "Bangalore", "Hyderabad"};

    // Method to fetch weather data from OpenWeatherMap API
    public static JSONObject getWeatherData(String city) throws Exception {
        String urlString = BASE_URL + city + "&appid=" + API_KEY;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();
        return new JSONObject(content.toString());
    }

    // Convert temperature from Kelvin to Celsius
    public static double kelvinToCelsius(double kelvin) {
        return kelvin - 273.15;
    }

    // Parse temperature from weather data
    public static double getTemperature(JSONObject weatherData) {
        return kelvinToCelsius(weatherData.getJSONObject("main").getDouble("temp"));
    }

    // Parse feels-like temperature
    public static double getFeelsLikeTemperature(JSONObject weatherData) {
        return kelvinToCelsius(weatherData.getJSONObject("main").getDouble("feels_like"));
    }

    // Parse weather condition (e.g., Rain, Clear)
    public static String getWeatherCondition(JSONObject weatherData) {
        return weatherData.getJSONArray("weather").getJSONObject(0).getString("main");
    }

    // Daily weather summary to calculate aggregates
    static class DailyWeatherSummary {
        private List<Double> temperatures = new ArrayList<>();
        private List<String> conditions = new ArrayList<>();

        public void addWeatherData(double temperature, String condition) {
            temperatures.add(temperature);
            conditions.add(condition);
        }

        public double getAverageTemperature() {
            return temperatures.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        }

        public double getMaxTemperature() {
            return temperatures.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
        }

        public double getMinTemperature() {
            return temperatures.stream().mapToDouble(Double::doubleValue).min().orElse(Double.NaN);
        }

        public String getDominantCondition() {
            return conditions.stream()
                    .reduce((a, b) -> conditions.stream().filter(c -> c.equals(a)).count() >= conditions.stream().filter(c -> c.equals(b)).count() ? a : b)
                    .orElse("Clear");
        }
    }

    // Alert system to check for threshold breaches
    static class AlertSystem {
        private double temperatureThreshold;

        public AlertSystem(double temperatureThreshold) {
            this.temperatureThreshold = temperatureThreshold;
        }

        public void checkForAlert(double currentTemperature) {
            if (currentTemperature > temperatureThreshold) {
                System.out.println("ALERT: Temperature has exceeded " + temperatureThreshold + "°C!");
            }
        }
    }

    public static void main(String[] args) {
        // Daily summary and alert system
        DailyWeatherSummary dailySummary = new DailyWeatherSummary();
        AlertSystem alertSystem = new AlertSystem(35);  // Set alert threshold to 35°C

        // Timer to fetch weather data at regular intervals
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    for (String city : CITIES) {
                        JSONObject weatherData = getWeatherData(city);
                        double temperature = getTemperature(weatherData);
                        String condition = getWeatherCondition(weatherData);

                        // Add data to daily summary
                        dailySummary.addWeatherData(temperature, condition);

                        // Check for temperature alerts
                        alertSystem.checkForAlert(temperature);

                        // Print current weather data for the city
                        System.out.println("City: " + city + " | Temperature: " + temperature + "°C | Condition: " + condition);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, FETCH_INTERVAL);

        // Hook to summarize daily data when the program ends
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nSummary for the Day:");
            System.out.println("Average Temperature: " + dailySummary.getAverageTemperature() + "°C");
            System.out.println("Maximum Temperature: " + dailySummary.getMaxTemperature() + "°C");
            System.out.println("Minimum Temperature: " + dailySummary.getMinTemperature() + "°C");
            System.out.println("Dominant Weather Condition: " + dailySummary.getDominantCondition());
        }));
    }
}

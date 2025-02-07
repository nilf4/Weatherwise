import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WeatherApp extends JFrame {
    private JTextField cityField;
    private JButton searchButton;
    private JTextArea resultTextArea;

    public WeatherApp() {
        setTitle("Weather App");
        setSize(1000, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        cityField = new JTextField(20);
        searchButton = new JButton("Search");
        resultTextArea = new JTextArea();

        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Enter City: "));
        inputPanel.add(cityField);
        inputPanel.add(searchButton);

        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultTextArea), BorderLayout.CENTER);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cityName = cityField.getText();
                if (!cityName.isEmpty()) {
                    String apiKey = "bd5e378503939ddaee76f12ad7a97608";
                    String apiUrl = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + apiKey;

                    String response = makeHttpRequest(apiUrl);
                    String weatherInfo = parseWeatherInfo(response);
                    resultTextArea.setText(weatherInfo);

                    saveWeatherDataToDatabase(cityName, response);
                } else {
                    resultTextArea.setText("Please enter a city name.");
                }
            }
        });
    }

    private void saveWeatherDataToDatabase(String cityName, String json) {
        String dbUrl = "jdbc:mysql://localhost/weather";
        String dbUsername = "root";
        String dbPassword = "Siddharth@mysql123";

        try {
            Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            String insertQuery = "INSERT INTO locations (city_name, temperature, humidity, clouds, weather_description, record_date) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String date = sdf.format(new Date());

            JSONObject jsonObject = new JSONObject(json);
            JSONObject main = jsonObject.getJSONObject("main");
            Double temperature = main.getDouble("temp") - 273.15;
            int humidity = main.getInt("humidity");
            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String description = weather.getString("description");
            JSONObject clouds = jsonObject.getJSONObject("clouds");
            int cloudiness = clouds.getInt("all");

            preparedStatement.setString(1, cityName);
            preparedStatement.setDouble(2, temperature);
            preparedStatement.setInt(3, humidity);
            preparedStatement.setInt(4, cloudiness);
            preparedStatement.setString(5, description);
            preparedStatement.setString(6, date);

            preparedStatement.executeUpdate();
            connection.close();
        } catch (SQLException | JSONException e) {
            e.printStackTrace();
        }
    }

    private String makeHttpRequest(String apiUrl) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        StringBuilder response = new StringBuilder();

        try {
            URL url = new URL(apiUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            } else {
                System.err.println("HTTP Request Failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }

        return response.toString();
    }

    private String parseWeatherInfo(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONObject main = jsonObject.getJSONObject("main");
            Double temperature = main.getDouble("temp") - 273.15;
            int humidity = main.getInt("humidity");
            JSONArray weatherArray = jsonObject.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);
            String description = weather.getString("description");
            JSONObject clouds = jsonObject.getJSONObject("clouds");
            int cloudiness = clouds.getInt("all");
            String cityName = jsonObject.getString("name");
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String formattedTemperature = decimalFormat.format(temperature);

            return "City: " + cityName + "\nTemperature: " + formattedTemperature + "°C" + "\nHumidity: " + humidity + "%" + "\nCloudiness: " + cloudiness + "%" + "\nWeather: " + description;
        } catch (JSONException e) {
            e.printStackTrace();
            return "Error parsing weather data.";
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new WeatherApp().setVisible(true);
            }
        });
    }
}

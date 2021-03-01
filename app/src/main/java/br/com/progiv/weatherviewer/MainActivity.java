package br.com.progiv.weatherviewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //lista de objetos Weather que representam a previsão
    private List<Weather> weatherList = new ArrayList<>();

    //ArrayAdapter para vincular objetos Weather a uma ListView
    private WeatherArrayAdapter weatherArrayAdapter;
    private ListView weatherListView; //exibe as informações de previsão

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //cria ArrayAdapter para vincular weatherList a weatherListView
        weatherListView = (ListView) findViewById(R.id.weatherListView);
        weatherArrayAdapter = new WeatherArrayAdapter(this, weatherList);
        weatherListView.setAdapter(weatherArrayAdapter);

        //Configura FAB para ocultar o teclado e iniciar a solicitação ao webservice
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //obtém texto de locationEditText e cria a URL do webservice
                EditText locationEditText = (EditText) findViewById(R.id.locationEditText);
                URL url = createURL(locationEditText.getText().toString());

                //oculta o teclado e inicia uma GetWeatherTask para o download
                // de dados climáticos de OpenWeatherMap.org em uma thread separada
                if(url != null){
                    dismissKeyboard(locationEditText);
                    GetWeatherTask getLocalWeatherTask = new GetWeatherTask();
                    getLocalWeatherTask.execute(url);
                }else{
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.invalid_url, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    //remove o teclado via código quando o usuário pressionar o botao FAB
    private void dismissKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(weatherListView.getWindowToken(), 0);
    }

    //cria a URL do webservice de openweathermap.org usando cidade:
    private URL createURL(String cidade){
        String apiKey = getString(R.string.api_key);
        String baseURL = getString(R.string.web_service_url);
        try{
            //cria a url para a cidade e para as unidade específicas (Fahrenheit)
            String urlString = baseURL + URLEncoder.encode(cidade, "UTF-8") + "&units=imperial&cnt=16&appid=" + apiKey; // para Celsius = metric
            return new URL(urlString);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    //faz a chamada ao webservice REST para obter os dados e os salva em um arquvio HTML local
    private class GetWeatherTask extends AsyncTask<URL, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(URL... urls) {
            HttpURLConnection connection = null;

            try{
                connection = (HttpURLConnection) urls[0].openConnection();
                int response = connection.getResponseCode();
                if(response == HttpURLConnection.HTTP_OK){
                    StringBuilder builder = new StringBuilder();
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))){
                        String line;
                        while((line = reader.readLine()) != null){
                            builder.append(line);
                        }
                    }catch (IOException e){
                        Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.read_error, Snackbar.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                    return new JSONObject(builder.toString());
                }else{
                    Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                }
            }catch (Exception e){
                Snackbar.make(findViewById(R.id.coordinatorLayout), R.string.connect_error, Snackbar.LENGTH_LONG).show();
                e.printStackTrace();
            }finally {
                connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            convertJSONtoArrayList(jsonObject); //preenche weatherList novamente
            weatherArrayAdapter.notifyDataSetChanged(); //vincula a listview novamente
            weatherListView.smoothScrollByOffset(0); //rola para o topo
        }
    }

    //cria objetos weather a partir do JSONObject que contém a previsão
    private void convertJSONtoArrayList(JSONObject forecast){
        weatherList.clear(); //apara os dados climáticos antigos
        try{
            //obtem a lista JSONArray da previsão
            JSONArray list = forecast.getJSONArray("list");
            //convert cada elemento da lista em um objeto Wather
            for(int i = 0; i < list.length(); ++i){
                JSONObject day = list.getJSONObject(i); //obtem os dados de um dia
                //obtem o JSONObject das temperaturas do dia ("temp")
                JSONObject temperatures = day.getJSONObject("temp");

                //obtém o JSONObject "weather" do dia para a descricao e para o ícone
                JSONObject weather = day.getJSONArray("weather").getJSONObject(0);

                //adiciona novo objeto Weather a wetherList:
                weatherList.add(new Weather(
                        day.getLong("dt"), //data e hora
                        temperatures.getDouble("min"),
                        temperatures.getDouble("max"),
                        day.getDouble("humidity"),
                        weather.getString("description"),
                        weather.getString("icon")
                ));
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }
}
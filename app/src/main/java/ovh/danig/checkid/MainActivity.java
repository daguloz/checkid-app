package ovh.danig.checkid;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    EditText etResponse;
    TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtiene las referencias a los elementos de la vista
        etResponse = (EditText) findViewById(R.id.etResponse);
        tvConnectionStatus = (TextView) findViewById(R.id.tvIsConnected);

        // Comprueba si hay conexión
        if(isConnected()){
            tvConnectionStatus.setBackgroundColor(0xFF00CC00);
            tvConnectionStatus.setText("Conectado.");
        }
        else{
            tvConnectionStatus.setText("No hay conexión.");
        }

        // Realiza la petición HTTP en un hilo separado para no bloquear la aplicación
        new HttpAsyncTask().execute("http://danig.ovh/checkid/index.php/app/read");
    }

    // Comprueba si la aplicación tiene conexión a internet.
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    // Realiza una petición HTTP de tipo POST y devuelve el resultado.
    public static String POST(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // Crea un HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            // Asigna varios valores a la petición POST
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("tag", "tagValue"));
            pairs.add(new BasicNameValuePair("id", "idValue"));
            pairs.add(new BasicNameValuePair("token", "tokenValue"));
            httpPost.setEntity(new UrlEncodedFormEntity(pairs));

            // Realiza la petición
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // Obtiene la respuesta y la convierte a String
            inputStream = httpResponse.getEntity().getContent();
            if(inputStream != null) {
                result = convertInputStreamToString(inputStream);
            }
            else {
                result = "Error en la petición HTTP.";
            }

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    // Convierte un inputStream en un String
    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null) {
            result += line;
        }

        inputStream.close();
        return result;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Realiza una petición HTTP en un hilo separado.
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return POST(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Datos recibidos.", Toast.LENGTH_LONG).show();
            etResponse.setText(result);
        }
    }
}

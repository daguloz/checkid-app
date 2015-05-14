package ovh.danig.checkid;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    TextView tvReadId;
    TextView tvResponse;
    TextView tvResponseRaw;
    TextView tvConnectionStatus;
    TextView tvNFCStatus;
    Button btnRegister;
    String nfcReadId;

    //private final String apiURL = "http://192.168.1.128/";
    private final String apiURL = "http://danig.ovh/checkid/index.php/";

    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtiene las referencias a los elementos de la vista
        tvResponse = (TextView) findViewById(R.id.tvResponse);
        tvResponseRaw = (TextView) findViewById(R.id.tvResponseRaw);
        tvReadId = (TextView) findViewById(R.id.tvReadId);
        tvConnectionStatus = (TextView) findViewById(R.id.tvIsConnected);
        tvNFCStatus = (TextView) findViewById(R.id.tvNFCStatus);
        btnRegister = (Button) findViewById(R.id.btnRegister);

        // Comprueba si hay conexión a internet
        if(isConnected()){
            tvConnectionStatus.setBackgroundColor(0xFF00CC00);
            tvConnectionStatus.setText("Servidor ON");
        }
        else{
            tvConnectionStatus.setText("Servidor OFF");
        }

        // Obtiene el controlador NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no dispone de NFC.", Toast.LENGTH_LONG).show();
        }

        if (!mNfcAdapter.isEnabled()) {
            tvNFCStatus.setText("NFC OFF");
        } else {
            tvNFCStatus.setBackgroundColor(0xFF00CC00);
            tvNFCStatus.setText("NFC ON");
        }

        // Manejar el intent si se obtiene uno (Se llama a esta funcion cuando el dispositivo lee un tag NFC)
        //handleIntent(getIntent());

    }

    @Override
    protected void onResume() {
        super.onResume();

        // Es necesario que la actividad esté activa para realizar la lectura. En caso contrario se
        // lanza una excepción de IllegalStateException
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        // Paramos la lectura cuando la actividad deja de estar activa.
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }



    @Override
    protected void onNewIntent(Intent intent) {
        // Maneja el intent si se obtiene uno (Se llama a esta funcion cuando el dispositivo lee un tag NFC)

        String action = intent.getAction();

        // El intent es una lectura TAG NFC?
        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {

            // Obtiene la ID del Tag y la convierte en Hexadecimal
            nfcReadId = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            tvReadId.setText("ID: " + nfcReadId);

            // Realiza una petición HTTP al servidor con la ID leída.
            // Esta petición se hace en un hilo separado para no bloquear la actividad.
            new HttpAsyncTaskGET().execute(apiURL + "/get/device/" + nfcReadId);
        }

    }

    // Convierte un array de bytes a un String Hexadecimal
    private String ByteArrayToHexString(byte [] inarray) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < inarray.length ; ++j)
        {
            in = (int) inarray[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        // creating pending intent
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][] {
                new String[] {
                        NfcA.class.getName(),
                        NfcB.class.getName(),
                        NfcF.class.getName(),
                        NfcV.class.getName(),
                        IsoDep.class.getName(),
                        MifareClassic.class.getName(),
                        MifareUltralight.class.getName(), Ndef.class.getName()
                }
        };

        // creating intent receiver for NFC events:
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);

        // Enabling foreground dispatch for getting intent from NFC event:
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
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

    // Realiza una petición HTTP de tipo GET y devuelve el resultado.
    public static String getDevice(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // Crea un HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(url);

            // Realiza la petición
            HttpResponse httpResponse = httpclient.execute(httpGet);

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

    // Realiza una petición HTTP de tipo POST y devuelve el resultado.
    public static String postDevice(String url, String tag){
        InputStream inputStream = null;
        String result = "";
        try {

            // Crea un HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            // Asigna varios valores a la petición POST
            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("tag", tag));
            pairs.add(new BasicNameValuePair("type", "unknown"));
            pairs.add(new BasicNameValuePair("user_id", null));
            pairs.add(new BasicNameValuePair("description", "Tag: " + tag));
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
    private class HttpAsyncTaskGET extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            return getDevice(urls[0]);
        }
        // onPostExecute muestra el resultado de AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Datos recibidos.", Toast.LENGTH_LONG).show();
            tvResponseRaw.setText(result);

            try {
                // Parseamos el JSON de respuesta...
                JSONObject jsonObj = new JSONObject(result);
                JSONArray deviceList = jsonObj.getJSONArray("device");
                if (deviceList.length() > 0){
                    JSONObject deviceData = deviceList.getJSONObject(0);
                    String description = deviceData.getString("description");
                    Log.d("API", "Dispositivo encontrando en base de datos externa! - " + nfcReadId);
                    tvResponse.setText(description);
                }
                else {
                    Log.d("API", "El dispositivo no es reconocido - " + nfcReadId);

                    tvResponse.setText("");

                    // Muestra un botón de registro si el dispositivo si no existe en la base de datos.
                    btnRegister.setVisibility(View.VISIBLE);
                    btnRegister.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            new HttpAsyncTaskPOST().execute(apiURL + "post/device/?type=unknown&user_id=1&description=Tag%20" + nfcReadId + "&tag=" + nfcReadId);
                        }
                    });
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private class HttpAsyncTaskPOST extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            //return postDevice(data[0], data[1]);
            return getDevice(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getBaseContext(), "Datos enviados.", Toast.LENGTH_LONG).show();
            tvResponseRaw.setText(result);

            try {
                JSONObject jsonObj = new JSONObject(result);

                // Parseamos el JSON de respuesta...
                String message = jsonObj.getString("message");
                if (message.equals("Success")){
                    Toast.makeText(getBaseContext(), "Dispositivo registrado!", Toast.LENGTH_LONG).show();
                    tvResponse.setText("Dispositivo registrado.");
                    btnRegister.setVisibility(View.INVISIBLE);
                }
                else {
                    Toast.makeText(getBaseContext(), "Error al registrar el dispositivo", Toast.LENGTH_LONG).show();
                    tvResponse.setText("Error al registrar el dispositivo");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}

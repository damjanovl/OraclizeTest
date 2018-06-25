package it.oraclize.oraclizefetch;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.renderscript.ScriptGroup;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.util.concurrent.ExecutionException;

import static it.oraclize.oraclizefetch.FetchActivity.resultView;

public class FetchActivity extends AppCompatActivity {

    private EditText urlInput;
    private Button fetchUrl;
    public static TextView resultView;
    public URL sanitizedURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fetch);

        urlInput = (EditText) findViewById(R.id.input_url);
        fetchUrl = (Button) findViewById(R.id.fetch_url);
        resultView = (TextView) findViewById(R.id.resultBox);
        resultView.setMovementMethod(new ScrollingMovementMethod());

        fetchUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlText = urlInput.getText().toString();
                if (validate(urlText)) {
                    resultView.setText("Url validated...\n");
                    hideKeyboard();
                    attemptDownload(urlText);
                } else {
                    resultView.setText("Entered an invalid Url!\n");
                }
            }
        });
    }

    public boolean validate(String url){
        resultView.setText("");
        // Attempt to create a proper URL
        try {
            sanitizedURL = new URL(url);
            return true;
        } catch (MalformedURLException mue) {
            Log.e("FETCH", "entered an improper url", mue);
            return false;
        }
    }

    public void attemptDownload(String url) {
        String result = null;
        UrlDownload request = new UrlDownload(getApplicationContext());

        try{
            request.execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            resultView.append("Failed to download page. Task interrupted.");
        } catch (ExecutionException e) {
            e.printStackTrace();
            resultView.append("Failed to download page. Task execution failed.");
        }
    }


    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}


class UrlDownload extends AsyncTask<String, Void, String> {
    public static final String METHOD = "GET";
    public static final int TIMEOUT = 15000;
    private final Context mContext;

    UrlDownload(final Context context) {
        // Set asynctask's context to application context
        mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {
        String requestUrl = params[0];
        String result;
        String inputLine;

        try {
            // Create the URL object with the passed url
            URL reqUrl = new URL(requestUrl);

            // Create the connection
            HttpURLConnection connection = (HttpURLConnection) reqUrl.openConnection();

            // Set connection method and timeouts
            connection.setRequestMethod(METHOD);
            connection.setReadTimeout(TIMEOUT);
            connection.setConnectTimeout(TIMEOUT);

            // Connect to url
            updateStatus("Connection established...\n");
            connection.connect();

            // Create a InputStreamReader
            InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

            // Create a buffered reader and string builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();

            // Check if the line we are reading is not null
            updateStatus("Reading data from page...\n");
            while((inputLine = reader.readLine()) != null) {
                stringBuilder.append(inputLine);
            }

            // Close inputstream and buffered reader
            reader.close();
            streamReader.close();

            result = stringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Failed to download page content for: " + requestUrl + "\n");
            result = null;
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error: " + e.toString() + "\n");
            result = null;
        }
        return result;
    }

    @Override
    public void onPostExecute(String result) {
        updateStatus("Finished downloading page content.\n");
        writeFile(result);
    }

    @Override
    protected void onPreExecute() {
        updateStatus("Creating Async Request Task...\n");
    }

    protected void updateStatus(String message) {
        FetchActivity.resultView.append(message);
    }

    private void writeFile(String result) {
        try {
            // Create a file
            final File path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM + "/Oraclize"
            );
            if (!path.exists()) {
                Log.d("DEBUG", "Creating directory: " + path.toString());
                path.mkdirs();
            }
            File outputFile = new File(path, "oraclize.txt");
            resultView.append("Creating file: " + outputFile.toString() + "...\n");

            // Create a new streamwrite with the temp file as the target
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);

            // Write to file then close it
            resultView.append("Writing to file...\n");
            outputStreamWriter.write(result);
            resultView.append("Writing to file complete.\n");
            outputStreamWriter.close();
            resultView.append("SUCCESS!");
        } catch (IOException e) {
            Log.e("Exception", "Failed to write to file: " + e.toString());
            resultView.append("Failed to write results to a temporary file.\n");
        }
    }
}
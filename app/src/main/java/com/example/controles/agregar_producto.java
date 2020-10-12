package com.example.controles;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;

public class agregar_producto extends AppCompatActivity {

    String resp, accion, id, rev;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_producto);

        try {
            FloatingActionButton btnMostrarProductos = (FloatingActionButton)findViewById(R.id.btnMostrarProductos);
            btnMostrarProductos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mostrarProductos();
                }
            });
            Button btnGuardaProducto = findViewById(R.id.btnGuardarProductos);
            btnGuardaProducto.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    guardarProducto();
                }
            });
            mostrarDatosProductos();
        }catch (Exception ex){
            Toast.makeText(getApplicationContext(), "Error al agregar Productos: "+ ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    void mostrarDatosProductos() {
        try {
            Bundle recibirParametros = getIntent().getExtras();
            accion = recibirParametros.getString("accion");
            if (accion.equals("Modificar")) {
                JSONObject dataProducto = new JSONObject(recibirParametros.getString("dataProducto")).getJSONObject("value");

                TextView tempVal = (TextView) findViewById(R.id.etNombreProducto);
                tempVal.setText(dataProducto.getString("nombre"));

                tempVal = findViewById(R.id.etMarcaProducto);
                tempVal.setText(dataProducto.getString("marca"));

                tempVal =  findViewById(R.id.etcantidadProducto);
                tempVal.setText(dataProducto.getString("cantidad"));

                tempVal =  findViewById(R.id.etPrecioProducto);
                tempVal.setText(dataProducto.getString("precio"));

                id = dataProducto.getString("_id");
                rev = dataProducto.getString("_rev");
            }
        } catch (Exception ex) {
            //
        }
    }
    private void mostrarProductos() {
        Intent mostrarProductos = new Intent(agregar_producto.this, MainActivity.class);
        startActivity(mostrarProductos);
    }

    private void guardarProducto() {
        TextView temval = findViewById(R.id.etNombreProducto);
        String nombre = temval.getText().toString();

        temval = findViewById(R.id.etMarcaProducto);
        String marca = temval.getText().toString();

        temval = findViewById(R.id.etcantidadProducto);
        String cantidad = temval.getText().toString();

        temval = findViewById(R.id.etPrecioProducto);
        String precio = temval.getText().toString();

        try {
            JSONObject datosProducto = new JSONObject();
            if (accion.equals("Modificar")) {
                datosProducto.put("_id", id);
                datosProducto.put("_rev", rev);
            }
            datosProducto.put("nombre", nombre);
            datosProducto.put("marca", marca);
            datosProducto.put("cantidad", cantidad);
            datosProducto.put("precio", precio);

            enviarDatosProductos objGuardarProducto = new enviarDatosProductos();
            objGuardarProducto.execute(datosProducto.toString());

        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Error de codigo" + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private class enviarDatosProductos extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;
        @Override
        protected String doInBackground(String... parametros) {
            StringBuilder stringBuilder = new StringBuilder();
            String jsonResponse = null;
            String jsonDatos = parametros[0];
            BufferedReader reader;

            try {
                URL url = new URL("Http://10.0.2.2:5984/db_tienda/");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Accept", "application/json");

                Writer writer = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream(), "UTF-8"));
                writer.write(jsonDatos);
                writer.close();

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));
                resp = reader.toString();

                String inputLine;
                StringBuffer stringBuffer = new StringBuffer();
                while ((inputLine = reader.readLine()) != null) {
                    stringBuffer.append(inputLine + "\n");
                }
                if (stringBuffer.length() == 0) {
                    return null;
                }
                jsonResponse = stringBuffer.toString();
                return jsonResponse;
            } catch (Exception ex) {
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                if (jsonObject.getBoolean("ok")) {
                    Toast.makeText(getApplicationContext(), "Datos guardado con exito", Toast.LENGTH_SHORT).show();
                    mostrarProductos();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al intentar guardar datos de producto", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar producto: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
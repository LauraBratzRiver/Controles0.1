package com.example.controles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    JSONArray datosJSON; // se guardan los datos
    JSONObject jsonObject;
    Integer posicion;
    ArrayList<String> arrayList = new ArrayList<String>();
    ArrayList<String> copyStringArrayList = new ArrayList<String>();
    ArrayAdapter<String> stringArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        obtenerDatosProductos objObtenerProductos = new obtenerDatosProductos();
        objObtenerProductos.execute();

        FloatingActionButton btnAgregarProductos = (FloatingActionButton) findViewById(R.id.btnAgregarProductos);
        btnAgregarProductos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                agregarNuevosProductos("Nuevo", jsonObject);
            }
        });
        buscarProductos();
    }

    void buscarProductos() {
        final TextView tempVal = findViewById(R.id.etBuscarProductos);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                arrayList.clear();
                if (tempVal.getText().toString().trim().length() < 1) { // No tenemos texto el cual mostrar
                    arrayList.addAll(copyStringArrayList);
                } else { // Realizamos la busqueda
                    for (String producto : copyStringArrayList) {
                        if (producto.toLowerCase().contains(tempVal.getText().toString().trim().toLowerCase())) {
                            arrayList.add(producto);
                        }
                    }
                }
                stringArrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_principal, menu);
        try {
            AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
            posicion = adapterContextMenuInfo.position;
            menu.setHeaderTitle(datosJSON.getJSONObject(posicion).getString("nombre"));
        } catch (Exception ex) {
            ///
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mnxAgregar:
                agregarNuevosProductos("Nuevo", jsonObject);
                return true;

            case R.id.mnxModificar:
                try {
                    agregarNuevosProductos("Modificar", datosJSON.getJSONObject(posicion));
                } catch (Exception ex) {
                }
                return true;

            case R.id.mnxEliminar:
                AlertDialog eliminarProduct = eliminarProducto();
                eliminarProduct.show();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    private class obtenerDatosProductos extends AsyncTask<Void, Void, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(Void... voids) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL("Http://10.0.2.2:5984/db_tienda/_design/tienda/_view/mi-tienda");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            } catch (Exception ex) {
                //
            }
            return result.toString();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                jsonObject = new JSONObject(s);
                datosJSON = jsonObject.getJSONArray("rows");
                mostrarDatosProductos();

            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, "Error al parcear los datos: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void mostrarDatosProductos() {
        ListView lvTienda = findViewById(R.id.listTienda);
        try {
            arrayList.clear();
            stringArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
            lvTienda.setAdapter(stringArrayAdapter);

            for (int i = 0; i < datosJSON.length(); i++) {
                stringArrayAdapter.add(datosJSON.getJSONObject(i).getJSONObject("value").getString("nombre"));
            }
            copyStringArrayList.clear();
            copyStringArrayList.addAll(arrayList);

            stringArrayAdapter.notifyDataSetChanged();
            registerForContextMenu(lvTienda);
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "Error al mostrar los productos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void agregarNuevosProductos(String accion, JSONObject jsonObject) {
        try {
            Bundle enviarParametros = new Bundle();
            enviarParametros.putString("accion", accion);
            enviarParametros.putString("dataProducto", jsonObject.toString());

            Intent agregarProductos = new Intent(MainActivity.this, agregar_producto.class);
            agregarProductos.putExtras(enviarParametros);
            startActivity(agregarProductos);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error al llamar agregar producto: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    AlertDialog eliminarProducto() {
        AlertDialog.Builder confirmacion = new AlertDialog.Builder(MainActivity.this);
        try {
            confirmacion.setTitle(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("nombre"));
            confirmacion.setMessage("¿Esta seguro de eliminar el producto?");
            confirmacion.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    eliminarDatosProducto objEliminarProducto = new eliminarDatosProducto();
                    objEliminarProducto.execute();

                    Toast.makeText(getApplicationContext(), "El producto se elimino con exito.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
            confirmacion.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(), "Se canselo la eliminacion.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Error al mostrar la confirmacion: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return confirmacion.create();
    }

    private class eliminarDatosProducto extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... parametros) {
            StringBuilder stringBuilder = new StringBuilder();
            String jsonResponse = null;
            try {
                URL url = new URL("http://10.0.2.2:5984/db_tienda/" +
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_id") + "?rev=" +
                        datosJSON.getJSONObject(posicion).getJSONObject("value").getString("_rev"));

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

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
                //
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
                    obtenerDatosProductos objObtenerProductos = new obtenerDatosProductos();
                    objObtenerProductos.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al intentar guardar datos de producto", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar producto: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}



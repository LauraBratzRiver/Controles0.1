package com.example.controles;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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

//https://developer.android.com/training/camera/photobasics?hl=es-419#java

public class MainActivity extends AppCompatActivity {

    DB miDB;
    Cursor misProductos;
    productos producto;
    ArrayList<productos> stringArrayList = new ArrayList<productos>();
    ArrayList<productos> copyStringArrayList = new ArrayList<productos>();
    ListView lvsProductos;

    JSONArray datosJSON;
    JSONObject jsonObject;
    Integer posicion;
    ArrayList<String> arrayList = new ArrayList<String>();
    ArrayList<String> CopyStringArrayList = new ArrayList<String>();
    ArrayAdapter<String> stringArrayAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ObtenerDatosProductos objObtenerProductos = new ObtenerDatosProductos();
        objObtenerProductos.execute();



        FloatingActionButton btnAgregarProductos = (FloatingActionButton)findViewById(R.id.btnAgregarProductos);
        btnAgregarProductos.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                agregarProducto("Nuevo", new String[]{} );

                agregarNuevosProductos("Nuevo", jsonObject);

            }
        });
        obtenerDatosProductos();
        buscarProductos();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_productos, menu);

        try {
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo)menuInfo;
        misProductos.moveToPosition((adapterContextMenuInfo.position));

        posicion = adapterContextMenuInfo.position;

        menu.setHeaderTitle(misProductos.getString(1));

        menu.setHeaderTitle(datosJSON.getJSONObject(posicion).getString("codigo"));
        } catch (Exception ex) {

        }
    }


    void buscarProductos(){
        final TextView tempVal = (TextView)findViewById(R.id.etBuscarProducto);
        tempVal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    stringArrayList.clear();

                    arrayList.clear();

                    if (tempVal.getText().toString().trim().length() < 1) { //aqui no hay texto para mostrar
                        stringArrayList.addAll(copyStringArrayList);

                        arrayList.addAll(CopyStringArrayList);


                    } else {
                        for (productos am : copyStringArrayList) {
                            String codigo = am.getDescripcion();
                            if (codigo.toLowerCase().contains(tempVal.getText().toString().trim().toLowerCase())) {
                                stringArrayList.add(am);
                            }
                        }
                    }
                    adaptadorImagenes adaptadorImg = new adaptadorImagenes(getApplicationContext(), stringArrayList);
                    lvsProductos.setAdapter(adaptadorImg);
                }catch (Exception ex){
                    Toast.makeText(getApplicationContext(), "Error: "+ ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.mnxAgregar:
                agregarProducto("Nuevo", new String[]{});

                agregarNuevosProductos("Nuevo", jsonObject);

                return true;

            case R.id.mnxModificar:


                try {
                    agregarNuevosProductos("Modificar", datosJSON.getJSONObject(posicion));
                } catch (Exception ex) {
                }


                String[] dataProducto = {
                        misProductos.getString(0),
                        misProductos.getString(1),
                        misProductos.getString(2),
                        misProductos.getString(3),
                        misProductos.getString(4),
                        misProductos.getString(5),
                        misProductos.getString(6) // Para la URL
                };
                agregarProducto("Modificar", dataProducto);
                return true;

            case R.id.mnxEliminar:
                AlertDialog eliminarProduct =  eliminarProductos();
                AlertDialog EliminarProduct =  EliminarProducto();

                eliminarProduct.show();
                EliminarProduct.show();

                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    AlertDialog EliminarProducto() {
        AlertDialog.Builder Confirmacion = new AlertDialog.Builder(MainActivity.this);
        try {
            Confirmacion.setTitle(datosJSON.getJSONObject(posicion).getJSONObject("value").getString("codigo"));
            Confirmacion.setMessage("¿Esta seguro de eliminar el producto?");
            Confirmacion.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    eliminarDatosProducto objEliminarProducto = new eliminarDatosProducto();
                    objEliminarProducto.execute();

                    Toast.makeText(getApplicationContext(), "El producto se elimino con exito.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
            Confirmacion.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(), "Se canselo la eliminacion.", Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Error al mostrar la confirmacion: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return Confirmacion.create();
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
                    Toast.makeText(getApplicationContext(), "Datos de producto guardado con exito", Toast.LENGTH_SHORT).show();
                    ObtenerDatosProductos objObtenerProductos = new ObtenerDatosProductos();
                    objObtenerProductos.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Error al intentar guardar datos de producto", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Error al guardar producto: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }


    AlertDialog eliminarProductos(){
        AlertDialog.Builder confirmacion = new AlertDialog.Builder(MainActivity.this);
        confirmacion.setTitle(misProductos.getString(1));
        confirmacion.setMessage("Esta seguro de eliminar el Producto?");
        confirmacion.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                miDB.mantenimientoProductos("Eliminar",new String[]{misProductos.getString(0)});
                obtenerDatosProductos();
                Toast.makeText(getApplicationContext(), "Producto eliminado exitosamente.",Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });
        confirmacion.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "Sea eliminado el producto.",Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });
        return confirmacion.create();
    }


    private class ObtenerDatosProductos extends AsyncTask<Void, Void, String> {
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
                MostrarDatosProductos();

            } catch (Exception ex) {
                Toast.makeText(MainActivity.this, "Error al parcear los datos: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    void obtenerDatosProductos(){
        miDB = new DB(getApplicationContext(), "", null, 1);
        misProductos = miDB.mantenimientoProductos( "Consultar", null);

        if(misProductos.moveToFirst()){ // si hay registros en la base de datos que mostrar
            mostrarDatosProductos();
        }else{
            Toast.makeText(getApplicationContext(),"No hay Productos que mostrar", Toast.LENGTH_SHORT).show();

            agregarProducto("Nuevo", new String[]{});
        }
    }

    private void agregarNuevosProductos(String accion, JSONObject jsonObject) {
        try {
            Bundle enviarParametros = new Bundle();
            enviarParametros.putString("accion", accion);
            enviarParametros.putString("dataProducto", jsonObject.toString());

            Intent agregarProductos = new Intent(MainActivity.this, agregarProductos.class);
            agregarProductos.putExtras(enviarParametros);
            startActivity(agregarProductos);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Error al llamar agregar producto: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    }



    void agregarProducto(String accion, String[] dataProducto){
        Bundle enviarParametros = new Bundle();
        enviarParametros.putString("accion", accion);
        enviarParametros.putStringArray("dataProducto", dataProducto);
        Intent agregarProductos = new Intent(MainActivity.this, agregarProductos.class);
        agregarProductos.putExtras(enviarParametros);
        startActivity(agregarProductos);
    }


    private void MostrarDatosProductos() {
        ListView lvTienda = findViewById(R.id.lvsProductos);
        try {
            arrayList.clear();
            stringArrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
            lvTienda.setAdapter(stringArrayAdapter);

            for (int i = 0; i < datosJSON.length(); i++) {
                stringArrayAdapter.add(datosJSON.getJSONObject(i).getJSONObject("value").getString("descripcion"));
            }
            CopyStringArrayList.clear();
            CopyStringArrayList.addAll(arrayList);

            stringArrayAdapter.notifyDataSetChanged();
            registerForContextMenu(lvTienda);
        } catch (Exception ex) {
            Toast.makeText(MainActivity.this, "Error al mostrar los productos: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    void mostrarDatosProductos() {
        stringArrayList.clear();
        lvsProductos = (ListView)findViewById(R.id.lvsProductos);
        do {
            producto = new productos(misProductos.getString(0), misProductos.getString(1),misProductos.getString(2), misProductos.getString(3), misProductos.getString(4), misProductos.getString(5), misProductos.getString(6));
            stringArrayList.add(producto);
        } while (misProductos.moveToNext());

        adaptadorImagenes adaptadorImg = new adaptadorImagenes(getApplicationContext(), stringArrayList);
        lvsProductos.setAdapter(adaptadorImg);

        copyStringArrayList.clear(); // Para hacer una limpieza de la lista
        copyStringArrayList.addAll(stringArrayList); //Para crear una copia de la lista de productos

        registerForContextMenu(lvsProductos);
    }
}

class productos{
    String id;
    String codigo;
    String descripcion;
    String marca;
    String precentacion;
    String precio;
    String urlImg;

    public productos(String id, String codigo, String descripcion, String marca, String precentacion, String precio, String urlImg) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.marca = marca;
        this.precentacion = precentacion;
        this.precio = precio;
        this.urlImg = urlImg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getPrecentacion() {
        return precentacion;
    }

    public void setPrecentacion(String precentacion) {
        this.precentacion = precentacion;
    }

    public String getPrecio() {
        return precio;
    }

    public void setPrecio(String precio) {
        this.precio = precio;
    }

    public String getUrlImg() {
        return urlImg;
    }

    public void setUrlImg(String urlImg) {
        this.urlImg = urlImg;
    }
}
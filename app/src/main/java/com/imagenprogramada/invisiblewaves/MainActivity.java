package com.imagenprogramada.invisiblewaves;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.imagenprogramada.invisiblewaves.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //codigo para intent de iniciar bluetooth
    private final int HABILITA_BT = 1;
    //codigo para peticion de permisos de localizacion
    private final int HABILITA_LOC = 2;

    //referencia al adaptador de bluetooth
    private BluetoothAdapter btAdapter;

    //estado de activado del adaptador
    boolean mActivado = false;

    //listado de dispositivos para la lista de dispositivos disponibles
    ArrayList<BluetoothDevice> listaDeDispositivos = new ArrayList<BluetoothDevice>();
    //adaptador de la recyclerview de lista de contactos

    //adaptador del recyclerview de dispositivos
    private DispositivosRecyclerViewAdapter adapter;

    //referencia al dispositivo seleccionado actualmente
    private BluetoothDevice dispositivoSeleccionado;

    //Referencia al dispositivo remoto conectado
    private BluetoothDevice dispositivoConectado;

    //binding de la view activity_main.xml
    private ActivityMainBinding binding;

    //Hilo del servidor
    private AcceptThread mAcceptThread;

    //Hilo del cliente
    private ConnectThread mConnectThread;

    //hilo para la comunicacion
    private ConnectedThread mConnectedThread;

    //estado actual
    private int estado;


    //escucha de eventos del bluetooth
    private BroadcastReceiver mReceiver;

    //Handler para comunicacion con hilos
    private Handler mHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        //inicializa eventos
        eventos();

        //inicializa listener  de Broadcast de bluetooth
        inicializarBroadcastReceiverBT();

        //inicializa handler para comunicar con los hilos
        inicializarHandler();

        //inicialiar lista de dispositivos
        RecyclerView recyclerView = (RecyclerView) binding.listaDispositivos;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DispositivosRecyclerViewAdapter(listaDeDispositivos, this);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Inicializa el broadcast receiver que se encarga de escuchar eventos de bluetooth durante
     * el descubrimiento de dispositivos
     */
    private void inicializarBroadcastReceiverBT() {
        mReceiver = new BroadcastReceiver() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("jjbo", "evento receiver");
                String action = intent.getAction();
                //dispositivo descubierto:
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    //Recoger el dispositivo BT:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //Agregarlo a la lista
                    listaDeDispositivos.add(device);
                    Log.i("jjbo", "Dispositivo encontrado");
                    //actualizar recicleview
                    adapter.notifyDataSetChanged();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //ocutar progressbar de descubrimento de diespositivos
                    binding.iconoBuscando.setVisibility(View.INVISIBLE);
                    Log.i("jjbo", "Busqueda terminada");
                }
            }
        };
    }


    /**
     * Inicializa el handler que se encarga de recibir mensajes de otros hilos
     */
    @SuppressLint("HandlerLeak")
    private void inicializarHandler() {
        mHandler = new Handler() {
            @SuppressLint("HandlerLeak")
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case Constantes.CAMBIAR_ESTADO -> {
                        if (msg.arg1 == Constantes.ESTADO_CONECTADO)
                            CambiarEstado(msg.arg1, msg.getData().getString(
                                    Constantes.NOMBRE_DISPOSITIVO));
                        else
                            CambiarEstado(msg.arg1, "Conectando");
                    }
                    case Constantes.MENSAJE_ENVIADO ->
                            CambiarEstado(Constantes.MENSAJE_ENVIADO, "");
                    case Constantes.MENSAJE_RECIBIDO -> {
                        byte[] readBuf = (byte[]) msg.obj;
                        //We build a string from the characters in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        binding.txtRecibir.setText(readMessage);
                        CambiarEstado(Constantes.MENSAJE_RECIBIDO, "");
                    }
                }
            }
        };
    }

    /**
     * Configuracion de eventos de los botones
     */
    private void eventos() {
        binding.btnIniciar.setOnClickListener(v -> iniciarBluetooth());
        binding.btnIniciarServidor.setOnClickListener(this::iniciarServidor);
        binding.btnIniciarCliente.setOnClickListener(this::iniciarCliente);
        binding.btnEnviar.setOnClickListener(this::enviar);
    }


    /**
     * Comprueba si el BT esta iniciado y en caso de no estarlo pide su habilitacion
     */
    @SuppressLint("MissingPermission")
    public void iniciarBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            //The device has no Bluetooth capabilities
            Toast.makeText(this, "No hay bluetooth", Toast.LENGTH_LONG).show();
        } else if (!btAdapter.isEnabled()) {
            //The device has Bluetooth capabilities, but they're not
            //enabled, so we request for them to be enabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, HABILITA_BT);
        } else {
            mActivado = true;
            buscarEmparejados();
        }

    }

    /**
     * Recoge la lista de dispositivos BT ya emparejados
     */
    @SuppressLint("NotifyDataSetChanged")
    private void buscarEmparejados() {
        listaDeDispositivos.clear();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        //If there are any bonded devices:
        if (pairedDevices.size() > 0)
            //Let's iterate bonded devices and add them to an ArrayAdapter
            listaDeDispositivos.addAll(pairedDevices);

        adapter.notifyDataSetChanged();
        descubreNuevosDispositivos();
    }

    /**
     * Lanza el descubrimiento de nuevos dispositivos bluetooth
     */
    public void descubreNuevosDispositivos() {
        //COMPROBAR PERMISOS DE BUSQUEDA
        if (
                ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED
                        ||
                        ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"}, HABILITA_LOC);
        } else {
            //SI SE TIENEN PERMISOS SE INTENTAN BUSCAR NUEVOS DISPOSITIVOS
            binding.iconoBuscando.setVisibility(View.VISIBLE);
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            this.registerReceiver(mReceiver, filter);
            // Register for broadcasts when discovery has finished
            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            this.registerReceiver(mReceiver, filter);

            if (btAdapter.isDiscovering()) {
                btAdapter.cancelDiscovery();
            }
            Log.i("jjbo", "Se va a lanzar startDiscovery");
            btAdapter.startDiscovery();
        }

    }

    /**
     * Recoge resultados de intents, Es usado para manejar el resultado de haber intentado
     * activar el bluetooth
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode The integer result code returned by the child activity
     *                   through its setResult().
     * @param data An Intent, which can return result data to the caller
     *               (various data can be attached to Intent "extras").
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == HABILITA_BT) {
            if (resultCode == RESULT_OK) {
                //Bluetooth was enabled
                Toast.makeText(this, R.string.activado, Toast.LENGTH_LONG).show();
                mActivado = true;
                iniciarBluetooth();
            } else {
                Toast.makeText(this, R.string.noactivado, Toast.LENGTH_LONG).show();
                mActivado = false;
            }
        }
    }


    /**
     * Maneja la respuesta de haber pedido permisos de acceso a posicion para acceder al descubrimiento del bluetooth:
     * -Si se han garantizado se lanza el descrubrimiento de dispositivos.
     * -Si no se ha garantizado pero no se ha marcado la casilla de "no preguntar otra vez" se le explica que es necesario
     * -Si ha marcado la casilla de "no preguntar otra vez" se le explica que el permiso es necesario y que puede
     *  activarlo manualmente en la configuración del teléfono
     *
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 1) {
            //si el permiso ha sido concedido
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                descubreNuevosDispositivos();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED || grantResults[1] == PackageManager.PERMISSION_DENIED) {
                //si no ha sido concedido entonces:
                // Si aún no ha marcado el no ser preguntado más se le da al usuario
                // una explicacion de la necesidad de dar el permiso
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.ACCESS_COARSE_LOCATION") || ActivityCompat.shouldShowRequestPermissionRationale(this, "android.permission.ACCESS_FINE_LOCATION")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.msg_explicacion).setTitle(R.string.aviso);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    //Si ha marcado que no quiere volver a ser preguntado se le explica que es imprescindible
                    //y que si quiere usar la aplicacion debe aceptarlo manualmente en la configuracion del telefono
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(R.string.msg_explicacion_manual).setTitle(R.string.aviso);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        }
    }

    /**
     * Selecciona un dispositivo de la lista de dispositivos
     * @param position
     */
    @SuppressLint({"MissingPermission", "SetTextI18n"})
    public void seleccionar(int position) {
        dispositivoSeleccionado = null;
        dispositivoSeleccionado = listaDeDispositivos.get(position);
        if (dispositivoSeleccionado != null) {
            binding.etiquetaDispositivoSeleccionado.setText("Dispositivo seleccionado: " + dispositivoSeleccionado.getName());
            Log.i("jjbo", dispositivoSeleccionado.getName());
        }
    }


    /**
     * Inicia el hilo de servidor lo cual pondra la app a la espera de recibir a un cliente
     * @param v
     */
    public void iniciarServidor(View v){
        if(btAdapter!=null) {
            mAcceptThread = new AcceptThread(btAdapter, mHandler, this);
            mAcceptThread.start();
        }
        else{
            Toast.makeText(this,"Por favor, dale al boton de iniciar",
                    Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Inicia el hilo de cliente lo cual iniciará el emparejado y permitira la comunicacion
     * @param v
     */
    public void iniciarCliente(View v){
        if(dispositivoSeleccionado==null)
            Toast.makeText(this, "elige un dispositivo al que conectarte primero",
                    Toast.LENGTH_LONG).show();
        else {
            dispositivoConectado=btAdapter.getRemoteDevice(dispositivoSeleccionado.getAddress());
            if(dispositivoConectado!=null) {
                mConnectThread = new ConnectThread(dispositivoConectado,
                        btAdapter, mHandler, this);
                mConnectThread.start();
            }
            else
                Toast.makeText(this, "no pude obtener enlace al dispositivo",
                        Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Envia un mensaje a traves del hilo ConnectedThread
     */
    public void enviar(View v){
        String mensaje = binding.txtEnviar.getText().toString();
        if (estado==Constantes.SIN_CONECTAR) {
            Toast.makeText(this, "conecta primero a un servidor!",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //Check if there's anything to send
        if (mensaje.length() > 0) {
            //Get it and send it
            byte[] send = mensaje.getBytes();
            mConnectedThread.write(send);
        }
    }

    /**
     * Inicia el hilo que maneja el envio y recepcion de la conexion
     */
    public synchronized void Conectar(BluetoothSocket socket) {
        //Connecting!!
        mConnectedThread = new ConnectedThread(socket,
                mHandler);
        mConnectedThread.start();
    }

    /**
     * Cambia el estado de la conexion
     */
    private void CambiarEstado(int tipo, String s) {
        estado=tipo;
        binding.outputEstado.setText(s);
        String stTipo ="";

        switch(tipo){
            case Constantes.ESTADO_CONECTANDO -> {stTipo="Conectando:";}
            case Constantes.ESTADO_CONECTADO -> {stTipo="Conectado:";}
            case Constantes.MENSAJE_ENVIADO -> {stTipo="Mensaje enviado:";}
            case Constantes.MENSAJE_RECIBIDO -> {stTipo="Mensaje recibido:";}
            case Constantes.CONEXION_PERDIDA -> {stTipo="Conexion peridad";}
        }

        Log.i("jjbo",""+stTipo+"-"+s);

    }
}
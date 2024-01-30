package com.imagenprogramada.invisiblewaves;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**
 * Hilo de cliente. Se encarga de iniciar la conexión
 */
public class ConnectThread extends Thread {
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final BluetoothSocket mmSocket;
    private final BluetoothAdapter mbtAdapter;
    private final MainActivity actividad;
    private final Handler mHandler;


    /**
     * Constructor. Crea un socket de cliente bluetooth para iniciar posteriormente la conexion
     */
    @SuppressLint("MissingPermission")
    public ConnectThread(BluetoothDevice device, BluetoothAdapter btAdapter,
                         Handler h, MainActivity act) {
        mbtAdapter=btAdapter;
        mHandler=h;
        actividad=act;
        BluetoothSocket tmp = null;
        //Get a BluetoothSocket to connect to the BluetoothDevice
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    /**
     * En la carrera se compienza la conexion y una vez iniciada la conexion se pide a la actividad
     * principal que inicie el manejo de la misma a traves del hilo ConnectedThread
     */
    @SuppressLint("MissingPermission")
    public void run() {
        //Cancel device discovery to speed up the connection
        mbtAdapter.cancelDiscovery();
        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
            return;
        }
        synchronized (this) {
            EnviarCambioEstado(Constantes.ESTADO_CONECTADO,
                    mmSocket.getRemoteDevice());
            actividad.Conectar(mmSocket);
        }
    }

    /**
     * Cancelar la conexion
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException ignored) { }
    }

    /**
     * Avisa a la actividad principal del cambio de estado
     */
    @SuppressLint("MissingPermission")
    public void EnviarCambioEstado(int i, BluetoothDevice device){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO,i,-1);
        //If there's a device available, data is sent as a Bundle
        if(device!=null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }
}
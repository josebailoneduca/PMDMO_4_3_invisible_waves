package com.imagenprogramada.invisiblewaves;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.util.UUID;

/**
 * Hilo de servidor crea un BluetoothSeverSocket a la espera de recibir alguna petición de conexion
 * Una vez conecta alguien se iniciará otro hilo de tipo ConectedThread que será el que maneje
 * los flujos de entrada y salida.
 */
public class AcceptThread extends Thread {
    private static final UUID MY_UUID =
            UUID.fromString("fa87c0d0-afac-11de-8a39-111111111111");
    private final static String NOMBRE_SERVICIO="miAppBluetooth";
    private final BluetoothServerSocket mmServerSocket;
    //handler de comunicacion con el hilo principal
    private final Handler mHandler;

    //actividad principal
    private final MainActivity actividad;

    /**
     * Constructor que crea el server socket
     * @param btAdapter Adaptador bluetooth
     * @param h Handler
     * @param act Actividad principal
     */
    @SuppressLint("MissingPermission")
    public AcceptThread(BluetoothAdapter btAdapter, Handler h, MainActivity act) {
        mHandler=h;
        actividad=act;
        BluetoothServerSocket tmp = null;
        try {
            tmp = btAdapter.listenUsingRfcommWithServiceRecord(NOMBRE_SERVICIO, MY_UUID);
        } catch (IOException ignored) { }
        mmServerSocket = tmp;
    }

    /**
     * En la carrera se queda escuchando el server socket. Una vez que alguien conecta
     * se lanza un hilo de tipo ConectedThread que manejará la recepccion de mensajes
     */
    public void run() {
        BluetoothSocket socket = null;
        //keep listening until a socket is accepted or an exception is launched
        while (true) {
            try {
                EnviarCambioEstado(Constantes.ESTADO_CONECTANDO,null);
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            //If a connection is accepted:
            if (socket != null) {
                synchronized (this) {
                    EnviarCambioEstado(Constantes.ESTADO_CONECTADO, socket.getRemoteDevice());
                    actividad.Conectar(socket);
                }
                break;
            }
        }
    }

    /**
     * Cierra la conexion
     */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException ignored) { }
    }

    /**
     * Avis a a la actividad principal del cambio de estado
     * @param i
     * @param device
     */
    @SuppressLint("MissingPermission")
    public void EnviarCambioEstado(int i, BluetoothDevice device){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO,i,-1);
        //If there's a device, data is sent as a Bundle
        if(device!=null) {
            Bundle bundle = new Bundle();
            bundle.putString(Constantes.NOMBRE_DISPOSITIVO, device.getName());
            msg.setData(bundle);
        }
        mHandler.sendMessage(msg);
    }
}
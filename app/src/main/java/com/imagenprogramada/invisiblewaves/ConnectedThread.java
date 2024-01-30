package com.imagenprogramada.invisiblewaves;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Hilo de conexion se encarga de recibir y enviar datos por los stream del socket de conexion
 */
public class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;
    private final Handler mHandler;

    /**
     * Constructor se encarga de sacar las referncias a los input y ouput streams del socket
     * @param socket
     * @param handler
     */
    public ConnectedThread(BluetoothSocket socket, Handler handler) {
        mmSocket = socket;
        mHandler=handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        //Get from the BluetoothSocket the input and output streams:
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException ignored) {}
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }

    /**
     * La carrera se queda permanentemente esperando mensajes recibidos pro el inputstream
     */
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;
        while (true) {
            try {
                //Read the InputStream
                bytes = mmInStream.read(buffer);
                //Menssage received!!
                mHandler.obtainMessage(Constantes.MENSAJE_RECIBIDO, bytes, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                EnviarCambioEstado(Constantes.CONEXION_PERDIDA);
                break;
            }
        }
    }

    /**
     * Avisa a la actividad principal del cambio de estado a traves del handler
     * @param i
     */
    public void EnviarCambioEstado(int i){
        Message msg = mHandler.obtainMessage(Constantes.CAMBIAR_ESTADO,i,-1);
        mHandler.sendMessage(msg);
    }


    /**
     * Escribir en el stream de salida del socket
     */
    public void write(byte[] buffer) {
        try {
            mmOutStream.write(buffer);
            //Menssage sent!!
            mHandler.obtainMessage(Constantes.MENSAJE_ENVIADO, -1, -1, buffer)
                    .sendToTarget();
        } catch (IOException ignored) {}
    }

    /**
     * Cerrar el socket
     */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException ignored) {}
    }
}
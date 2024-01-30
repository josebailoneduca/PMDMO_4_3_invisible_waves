package com.imagenprogramada.invisiblewaves;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


    /**
     * Adaptador para la lista de dispositivos
     */
    public class DispositivosRecyclerViewAdapter extends RecyclerView.Adapter<DispositivosRecyclerViewAdapter.ViewHolder> {

        /**
         * Lista de dispositivos a mostrar
         */
        private final List<BluetoothDevice> listaDispositivos;

        /**
         * Referencia a la actividad principal
         */
        private final MainActivity actividad;

        /**
         * Constructor
         * @param items Lista de dispositivos
         * @param actividad Referencia a la actividad
         */
        public DispositivosRecyclerViewAdapter(List<BluetoothDevice> items, MainActivity actividad) {
            listaDispositivos = items;
            this.actividad=actividad;
        }


        /**
         * Creacion de vistas
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         */
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View vista = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_de_lista,parent,false);
            return new ViewHolder(vista);
        }

        /**
         * Rellenar una vista con los datos que tocan
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @SuppressLint({"MissingPermission", "SetTextI18n"})
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            BluetoothDevice d = listaDispositivos.get(position);
            holder.dispositivo = d;
            holder.nombre.setText(d.getName()+"\n"+d.getAddress());
            //en una pulsacion larga se manda el sms
            holder.nombre.setOnClickListener(v -> {
                actividad.seleccionar(position);
            });
        }



        @Override
        public int getItemCount() {
            return listaDispositivos.size();
        }


        /**
         * Clase de vistas de items
         */
        public class ViewHolder extends RecyclerView.ViewHolder {
            public BluetoothDevice dispositivo;
            public final TextView nombre;


            public ViewHolder(View vista) {
                super(vista);
                nombre = vista.findViewById(R.id.nombre);
            }
        }
}

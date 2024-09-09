package com.example.chat;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    private List<WifiP2pDevice> peerList;
    private OnPeerClickListener listener;

    public interface OnPeerClickListener {
        void onPeerClick(WifiP2pDevice device);
    }

    public PeerListAdapter(List<WifiP2pDevice> peerList, OnPeerClickListener listener) {
        this.peerList = peerList;
        this.listener = listener;
    }

    public void updatePeerList(List<WifiP2pDevice> peers) {
        this.peerList = peers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new PeerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        final WifiP2pDevice device = peerList.get(position);
        holder.peerName.setText(device.deviceName);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onPeerClick(device);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return peerList != null ? peerList.size() : 0;
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView peerName;

        PeerViewHolder(View itemView) {
            super(itemView);
            peerName = itemView.findViewById(android.R.id.text1);
        }
    }
}

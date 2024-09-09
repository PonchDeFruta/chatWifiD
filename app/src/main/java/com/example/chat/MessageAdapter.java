package com.example.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class Message {
    String message;
    boolean isSent; // true if sent, false if received

    Message(String message, boolean isSent) {
        this.message = message;
        this.isSent = isSent;
    }
}

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messagesList;

    public MessageAdapter(List<Message> messages) {
        this.messagesList = messages;
    }

    public void addMessage(Message message) {
        messagesList.add(message);
        notifyItemInserted(messagesList.size() - 1);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        if (viewType == 1) { // Sent message
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else { // Received message
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messagesList.get(position);
        holder.messageText.setText(msg.message);
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return messagesList.get(position).isSent ? 1 : 0;
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
        }
    }
}

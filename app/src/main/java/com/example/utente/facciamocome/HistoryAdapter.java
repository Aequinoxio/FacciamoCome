package com.example.utente.facciamocome;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by utente on 14/06/2016.
 */
public class HistoryAdapter<FrasiTempo> extends ArrayAdapter {
    // ArrayAdapter(Context context, int resource, int textViewResourceId, T[] objects)
    List<MainActivity.FrasiTempo> obj;
    public HistoryAdapter(Context context, int textViewResourceId, List<MainActivity.FrasiTempo> objects) {
        super(context, textViewResourceId, objects);
        obj=objects;
    }

    @Override
    public int getCount() {
        return obj.size(); //DemoOption.values().length;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_next_thing, parent, false);
        }
     //   DemoOption currentOption = DemoOption.values()[position];
        ((TextView) convertView.findViewById(R.id.txtPhraseListItem)).setText(obj.get(position).phrase);
        ((TextView) convertView.findViewById(R.id.txtTimeListItem)).setText(obj.get(position).created_at);
        ((TextView) convertView.findViewById(R.id.txtPosition)).setText(String.valueOf(position+1));
        return convertView;
    }
}
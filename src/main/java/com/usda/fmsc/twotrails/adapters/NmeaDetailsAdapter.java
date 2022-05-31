package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.usda.fmsc.geospatial.nmea41.NmeaIDs;
import com.usda.fmsc.twotrails.R;

import java.util.ArrayList;
import java.util.List;

public class NmeaDetailsAdapter extends ArrayAdapter<NmeaDetailsAdapter.NmeaDetails> {
    private final List<NmeaDetails> nmeaDetails;

    private final LayoutInflater inflater;


    public NmeaDetailsAdapter(Context context) {
        this(context, new ArrayList<>());
    }

    public NmeaDetailsAdapter(Context context, List<NmeaDetails> nmeaDetails) {
        super(context, 0);
        this.nmeaDetails = nmeaDetails;

        inflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView,@NonNull ViewGroup parent) {
        ViewHolder holder;
        NmeaDetails details = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_nmea_info, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (details != null) {
            holder.talkerID.setText(details.getTalkerIdStr());
            holder.nmeaIDs.setText(details.getNmeaIdsStr());
        }

        return convertView;
    }


    @Override
    public NmeaDetails getItem(int position) {
        return nmeaDetails.get(position);
    }

    @Override
    public int getCount() {
        return nmeaDetails.size();
    }

    public static class NmeaDetails {
        private final String talkerIdStr;
        private final List<String> ids = new ArrayList<>();

        public NmeaDetails(NmeaIDs.TalkerID talkerID) {
            this(talkerID.toStringCode());
        }

        public NmeaDetails(String talkerIdStr) {
            this.talkerIdStr = talkerIdStr;
        }

        public boolean addId(NmeaIDs.SentenceID id) {
            if (!ids.contains(id.toString())) {
                ids.add(id.toString());
                return true;
            }

            return false;
        }

        public boolean addId(String id) {
            if (!ids.contains(id)) {
                ids.add(id);
                return true;
            }
            return false;
        }

        public String getTalkerIdStr() {
            return talkerIdStr;
        }

        public String getNmeaIdsStr(){
            StringBuilder sb = new StringBuilder();

            if (ids.size() > 0) {
                for (int i = 0; i < ids.size() - 1; i++) {
                    sb.append(String.format("%s, ", ids.get(i)));
                }

                sb.append(ids.get(ids.size() - 1));
            } else {
                sb.append("No NMEA IDs");
            }

            return sb.toString();
        }
    }


    private static class ViewHolder {
        public final TextView talkerID;
        public final TextView nmeaIDs;

        public ViewHolder(View view)
        {
            talkerID = view.findViewById(R.id.listRowTalkerId);
            nmeaIDs = view.findViewById(R.id.listRowNmeaIds);
        }
    }
}

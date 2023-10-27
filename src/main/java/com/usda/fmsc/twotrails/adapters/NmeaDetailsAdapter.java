package com.usda.fmsc.twotrails.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.usda.fmsc.geospatial.nmea.codes.SentenceID;
import com.usda.fmsc.geospatial.nmea.codes.TalkerID;
import com.usda.fmsc.twotrails.R;

import java.util.ArrayList;
import java.util.List;

public class NmeaDetailsAdapter extends ArrayAdapter<NmeaDetailsAdapter.GnssNmeaDetails> {
    private final List<GnssNmeaDetails> gnssNmeaDetails;

    private final LayoutInflater inflater;


    public NmeaDetailsAdapter(Context context) {
        this(context, new ArrayList<>());
    }

    public NmeaDetailsAdapter(Context context, List<GnssNmeaDetails> gnssNmeaDetails) {
        super(context, 0);
        this.gnssNmeaDetails = gnssNmeaDetails;

        inflater = LayoutInflater.from(context);
    }


    @NonNull
    @Override
    public View getView(int position, View convertView,@NonNull ViewGroup parent) {
        ViewHolder holder;
        GnssNmeaDetails details = getItem(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.list_row_nmea_info, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (details != null) {
            holder.talkerID.setText(details.getTalkerIdStr());
            holder.sentenceIDs.setText(details.getSentenceIdsStr());
        }

        return convertView;
    }


    @Override
    public GnssNmeaDetails getItem(int position) {
        return gnssNmeaDetails.get(position);
    }

    @Override
    public int getCount() {
        return gnssNmeaDetails.size();
    }

    public static class GnssNmeaDetails {
        private final String talkerIdStr;
        private final List<String> sentenceIds = new ArrayList<>();

        public GnssNmeaDetails(TalkerID talkerID) {
            this(talkerID.toStringCode());
        }

        public GnssNmeaDetails(String talkerIdStr) {
            this.talkerIdStr = talkerIdStr;
        }

        public boolean addSentenceId(SentenceID id) {
            if (!sentenceIds.contains(id.toString())) {
                sentenceIds.add(id.toString());
                return true;
            }

            return false;
        }

        public boolean addSentenceId(String id) {
            if (!sentenceIds.contains(id)) {
                sentenceIds.add(id);
                return true;
            }
            return false;
        }

        public String getTalkerIdStr() {
            return talkerIdStr;
        }

        public String getSentenceIdsStr(){
            StringBuilder sb = new StringBuilder();

            if (sentenceIds.size() > 0) {
                for (int i = 0; i < sentenceIds.size() - 1; i++) {
                    sb.append(String.format("%s, ", sentenceIds.get(i)));
                }

                sb.append(sentenceIds.get(sentenceIds.size() - 1));
            } else {
                sb.append("No NMEA IDs");
            }

            return sb.toString();
        }
    }


    private static class ViewHolder {
        public final TextView talkerID;
        public final TextView sentenceIDs;

        public ViewHolder(View view)
        {
            talkerID = view.findViewById(R.id.listRowTalkerId);
            sentenceIDs = view.findViewById(R.id.listRowNmeaIds);
        }
    }
}

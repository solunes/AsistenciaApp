package com.solunes.asistenciaapp.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.solunes.asistenciaapp.ItemSchedule;
import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.Schedule;
import com.solunes.asistenciaapp.utils.StringUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by jhonlimaster on 21-12-16.
 */

public class ScheduleRecyclerViewAdapter extends RecyclerView.Adapter<ScheduleRecyclerViewAdapter.ViewHolder> {

    private Context context;
    private ArrayList<ItemSchedule> itemSchedules;

    public ScheduleRecyclerViewAdapter(Context context, ArrayList<ItemSchedule> items) {
        this.context = context;
        itemSchedules = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ItemSchedule itemSchedule = itemSchedules.get(position);
        Date date = StringUtils.formateStringFromDate(StringUtils.DATE_FORMAT, itemSchedule.getDate());
        String fromstring = StringUtils.formateDateFromstring(StringUtils.HUMAN_DATE_FORMAT2, date);
        String fromstring2 = StringUtils.formateDateFromstring(StringUtils.HUMAN_DATE_FORMAT3, date);
        holder.dayNumber.setText(fromstring);
        holder.dayText.setText(fromstring2);
        for (Schedule schedule : itemSchedule.getSchedules()) {
            View view = LayoutInflater.from(context).inflate(R.layout.layout_hours, null);
            TextView textIn = (TextView) view.findViewById(R.id.text_schedule_in);
            textIn.setText(schedule.getIn());
            TextView textOut = (TextView) view.findViewById(R.id.text_schedule_out);
            textOut.setText(schedule.getOut());
            holder.layoutSchedule.addView(view);
            for (String textObs : schedule.getObservations()) {
                TextView textViewObs = (TextView) LayoutInflater.from(context).inflate(R.layout.obs_textview, null);
                textViewObs.setText(textObs);
                holder.layoutSchedule.addView(textViewObs);
            }
        }
    }

    @Override
    public int getItemCount() {
        return itemSchedules.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView dayNumber;
        final TextView dayText;
        LinearLayout layoutSchedule;

        ViewHolder(View view) {
            super(view);
            mView = view;
            dayNumber = (TextView) view.findViewById(R.id.day_number);
            dayText = (TextView) view.findViewById(R.id.day_text);
            layoutSchedule = (LinearLayout) view.findViewById(R.id.item_schedule);
        }

        @Override
        public String toString() {
            return super.toString() + "'";
        }
    }
}
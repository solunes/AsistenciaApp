package com.solunes.asistenciaapp.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.solunes.asistenciaapp.ItemSchedule;
import com.solunes.asistenciaapp.R;
import com.solunes.asistenciaapp.Schedule;
import com.solunes.asistenciaapp.utils.StringUtils;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by jhonlimaster on 21-12-16.
 */

public class ScheduleRecyclerViewAdapter extends SectionedRecyclerViewAdapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ScheduleRecyclerViewAda";
    private Context context;
    private ArrayList<ItemSchedule> itemSchedules;

    public ScheduleRecyclerViewAdapter(Context context, ArrayList<ItemSchedule> items) {
        this.context = context;
        itemSchedules = items;
    }

    @Override
    public int getItemViewType(int section, int relativePosition, int absolutePosition) {
        return super.getItemViewType(section, relativePosition, absolutePosition);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout;
        View view;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                layout = R.layout.layout_header;
                view = LayoutInflater.from(parent.getContext())
                        .inflate(layout, parent, false);
                return new ViewHolderHeader(view);
            case VIEW_TYPE_ITEM:
                layout = R.layout.layout_hours;
                view = LayoutInflater.from(parent.getContext())
                        .inflate(layout, parent, false);
                return new ViewHolder(view);
            default:
//                layout = R.layout.item_schedule;
                return null;
        }
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder holder, int section) {
        ViewHolderHeader holderHeader = (ViewHolderHeader) holder;
        ItemSchedule itemSchedule = itemSchedules.get(section);
        Date date = StringUtils.formateStringFromDate(StringUtils.DATE_FORMAT, itemSchedule.getDate());
        String fromstring = StringUtils.formateDateFromstring(StringUtils.HUMAN_DATE_FORMAT, date);
        holderHeader.dayText.setText(fromstring);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ViewHolder viewHolder = (ViewHolder) holder;
        ItemSchedule itemSchedule = itemSchedules.get(section);
        final Schedule schedule = itemSchedule.getSchedules().get(relativePosition);
        viewHolder.textScheduleIn.setText(schedule.getIn());
        viewHolder.textScheduleOut.setText(schedule.getOut());
        viewHolder.iconScheduleIn.setImageResource(setStatusIcon(schedule.getStatusIn()));
        viewHolder.iconScheduleOut.setImageResource(setStatusIcon(schedule.getStatusOut()));
        viewHolder.iconScheduleIn.setColorFilter(ContextCompat.getColor(context, setStatusBackground(schedule.getStatusIn())));
        viewHolder.iconScheduleOut.setColorFilter(ContextCompat.getColor(context, setStatusBackground(schedule.getStatusOut())));
        String obs = "";
//        String obs = "S: " + section + " RP: " + relativePosition + " AP: " + absolutePosition;

        for (String textObs : schedule.getObservations()) {
            obs += textObs + "\n";
        viewHolder.textObs.setVisibility(View.VISIBLE);
        }
        viewHolder.textObs.setText(obs.trim());
    }

    private int setStatusIcon(String status) {
        switch (status) {
            case "check":
                return R.drawable.ic_check_black_24dp;
            case "holding":
                return R.drawable.ic_cancel_black_24dp;
            case "pending":
                return R.drawable.ic_cancel_black_24dp;
            default:
                return 0;
        }
    }

    private int setStatusBackground(String status) {
        switch (status) {
            case "check":
                return R.color.colorCheck;
            case "holding":
                return R.color.colorHolding;
            case "pending":
                return R.color.colorHolding;
            default:
                return 0;
        }
    }

    @Override
    public int getSectionCount() {
        return this.itemSchedules.size();
    }

    @Override
    public int getItemCount(int section) {
        return itemSchedules.get(section).getSchedules().size();
    }

    private class ViewHolderHeader extends RecyclerView.ViewHolder {
        final TextView dayText;

        ViewHolderHeader(View view) {
            super(view);
            dayText = (TextView) view.findViewById(R.id.day_text);
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textScheduleIn;
        final ImageView iconScheduleIn;
        final TextView textScheduleOut;
        final ImageView iconScheduleOut;
        final TextView textObs;

        ViewHolder(View view) {
            super(view);
            textScheduleIn = (TextView) view.findViewById(R.id.text_schedule_in);
            iconScheduleIn = (ImageView) view.findViewById(R.id.icon_schedule_in);
            textScheduleOut = (TextView) view.findViewById(R.id.text_schedule_out);
            iconScheduleOut = (ImageView) view.findViewById(R.id.icon_schedule_out);
            textObs = (TextView) view.findViewById(R.id.obs_text);
        }
    }
}

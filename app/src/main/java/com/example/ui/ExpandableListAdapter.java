package com.example.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private Context context;
    private List<String> listGroup;
    private HashMap<String, List<String>> listChild;
    private HashMap<String, Boolean> checkStates;
    private OnCheckBoxClickListener listener;

    public interface OnCheckBoxClickListener {
        void onCheckBoxClicked(String groupItem, boolean isChecked);
    }

    public ExpandableListAdapter(Context context, List<String> listGroup,
                                 HashMap<String, List<String>> listChild,
                                 OnCheckBoxClickListener listener) {
        this.context = context;
        this.listGroup = listGroup;
        this.listChild = listChild;
        this.listener = listener;
        this.checkStates = new HashMap<>();

        // 초기 체크박스 상태 설정
        for (String group : listGroup) {
            checkStates.put(group, false);
        }
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String groupTitle = (String) getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);
        }

        TextView txtGroup = convertView.findViewById(R.id.listGroup);
        CheckBox checkBox = convertView.findViewById(R.id.groupCheckbox);

        txtGroup.setText(groupTitle);
        checkBox.setChecked(checkStates.get(groupTitle));

        // 체크박스 클릭 리스너
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = ((CheckBox) v).isChecked();
                checkStates.put(groupTitle, isChecked);
                if (listener != null) {
                    listener.onCheckBoxClicked(groupTitle, isChecked);
                }
            }
        });

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        String childText = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item, null);
        }

        TextView txtChild = convertView.findViewById(R.id.listItem);
        txtChild.setText(childText);

        return convertView;
    }

    // 기타 필수 메소드 구현
    @Override
    public int getGroupCount() {
        return listGroup.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return listChild.get(listGroup.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return listGroup.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return listChild.get(listGroup.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
package com.example.ui.ui.stretchmarket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ui.ExpandableListAdapter;
import com.example.ui.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class stretchMarketFragment extends Fragment {
    private ExpandableListView expandableListView;
    private ExpandableListAdapter expandableListAdapter;
    private List<String> listGroup;
    private HashMap<String, List<String>> listChild;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_stretchmarket, container, false);
        expandableListView = rootView.findViewById(R.id.expandableListView);

        if (expandableListView == null) {
            Toast.makeText(getContext(), "ExpandableListView를 찾을 수 없습니다.", Toast.LENGTH_LONG).show();
            return rootView;
        }

        prepareListData();
        expandableListAdapter = new ExpandableListAdapter(getContext(), listGroup, listChild,
                new ExpandableListAdapter.OnCheckBoxClickListener() {
                    @Override
                    public void onCheckBoxClicked(String groupItem, boolean isChecked) {
                        Toast.makeText(getContext(),
                                groupItem + "가 " + (isChecked ? "체크되었습니다." : "체크 해제되었습니다."),
                                Toast.LENGTH_SHORT).show();
                    }
                });

        expandableListView.setAdapter(expandableListAdapter);
        return rootView;
    }

    private void prepareListData() {
        listGroup = new ArrayList<>();
        listChild = new HashMap<>();

        // 그룹 추가
        listGroup.add("스트레칭 1-10");
        listGroup.add("스트레칭 11-20");

        // 첫 번째 그룹의 차일드 아이템
        List<String> list1 = new ArrayList<>();
        list1.add("Item 1.1");
        list1.add("Item 1.2");
        list1.add("Item 1.3");

        // 두 번째 그룹의 차일드 아이템
        List<String> list2 = new ArrayList<>();
        list2.add("Item 2.1");
        list2.add("Item 2.2");
        list2.add("Item 2.3");

        listChild.put(listGroup.get(0), list1);
        listChild.put(listGroup.get(1), list2);
    }
}
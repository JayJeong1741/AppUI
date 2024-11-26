package com.example.ui.ui.myinfo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.ui.R;
import com.example.ui.database.FirebaseManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class myinfoFragment extends Fragment {
    private BarChart todayBarChart;
    private LineChart weeklyLineChart;
    private FirebaseManager firebaseManager;
    private   int totalDetectionCount = 0; // 하루 총 감지 횟수를 계산할 변수
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_my_info, container, false);

        firebaseManager = new FirebaseManager();

        todayBarChart = rootView.findViewById(R.id.today_bar_chart);
        weeklyLineChart = rootView.findViewById(R.id.weekly_line_chart);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("USER_ID", null);

        if (userId != null) {
            firebaseManager.readDataById("turtleneck", userId, new FirebaseManager.FirebaseCallback() {
                @Override
                public void onSuccess(Object data) {
                    Object[] results = (Object[]) data;
                    Map<String, Integer> hourlyData = (Map<String, Integer>) results[0];
                    Map<String, Integer> dailyData = (Map<String, Integer>) results[1];
                    double averageDailyCount = (double) results[2];

                    updateBarChart(hourlyData);
                    updateLineChart(dailyData);
                    Log.d("myinfoFragment", "hourlyData: " + hourlyData.toString());
                    Log.d("myinfoFragment", "dailyData: " + dailyData.toString());
                    TextView user = rootView.findViewById(R.id.user_name);
                    user.setText(userId +" 님");
                    TextView todayDetection = rootView.findViewById(R.id.today_chart_label);
                    TextView weeklyAverageLabel = rootView.findViewById(R.id.weekly_average_label);
                    // 하루 총 감지 횟수를 TextView에 설정
                    todayDetection.setText(String.format(Locale.getDefault(), "오늘 총 감지 횟수: %d 회", totalDetectionCount));

                    weeklyAverageLabel.setText(String.format(Locale.getDefault(), "평균 감지횟수: %.1f 회", averageDailyCount));
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e("FirebaseManager", "데이터 로드 실패", e);
                }
            });
        }

        return rootView;
    }

    private void updateBarChart(Map<String, Integer> hourlyData) {
        List<BarEntry> entries = new ArrayList<>();


        // 시간 데이터 채우기 (0~23시)
        for (int hour = 0; hour < 24; hour++) {
            String hourKey = String.format(Locale.getDefault(), "%02d", hour); // 02, 03 같은 형식 보장
            int count = hourlyData.getOrDefault(hourKey, 0);
            totalDetectionCount += count; // 총 횟수 합산
            entries.add(new BarEntry(hour, count));
        }

        // 데이터 세트 생성
        BarDataSet dataSet = new BarDataSet(entries, "시간별 감지 횟수");
        dataSet.setColor(Color.BLUE);

        // 데이터 설정
        BarData barData = new BarData(dataSet);
        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // 정수로 표시
            }
        });

        todayBarChart.setData(barData);

        // X축 설정
        XAxis xAxis = todayBarChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelCount(24); // 0~23시 라벨 설정
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%02d", (int) value); // 02, 03 등 형식
            }
        });

        // Y축 설정
        YAxis leftAxis = todayBarChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // 0부터 시작
        leftAxis.setGranularity(1f);

        todayBarChart.getAxisRight().setEnabled(false); // 오른쪽 Y축 비활성화
        todayBarChart.getDescription().setEnabled(false); // 설명 비활성화
        todayBarChart.invalidate(); // 차트 갱신
    }
    private void updateLineChart(Map<String, Integer> dailyData) {
        Map<String, Integer> fullData = fillMissingDates(dailyData);

        List<Entry> entries = new ArrayList<>();
        List<String> xLabels = new ArrayList<>();

        int index = 0;
        for (Map.Entry<String, Integer> entry : fullData.entrySet()) {
            xLabels.add(entry.getKey()); // 이미 MM-DD 포맷으로 저장된 값 사용
            entries.add(new Entry(index++, entry.getValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "날짜별 감지 횟수");
        dataSet.setColor(Color.rgb(255, 165, 0)); // 주황색 설정
        dataSet.setCircleColor(Color.rgb(255, 165, 0));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(3f);

        LineData lineData = new LineData(dataSet);
        lineData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value); // 정수로 표시
            }
        });

        weeklyLineChart.setData(lineData);

        // X축 설정
        XAxis xAxis = weeklyLineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        // Y축 설정
        YAxis leftAxis = weeklyLineChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f); // 0부터 시작
        leftAxis.setGranularity(1f);

        weeklyLineChart.getAxisRight().setEnabled(false); // 오른쪽 Y축 비활성화
        weeklyLineChart.setExtraRightOffset(15f); // 오른쪽 여백 추가
        weeklyLineChart.getDescription().setEnabled(false); // 설명 비활성화
        weeklyLineChart.invalidate();
    }
    private Map<String, Integer> fillMissingDates(Map<String, Integer> dailyData) {
        // 현재 날짜를 기준으로 7일치 데이터 생성
        Map<String, Integer> fullData = new LinkedHashMap<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());

        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String date = originalFormat.format(calendar.getTime());
            String formattedDate = displayFormat.format(calendar.getTime());
            fullData.put(formattedDate, dailyData.getOrDefault(date, 0));
            calendar.add(Calendar.DAY_OF_MONTH, i); // 날짜 복원
        }
        return fullData;
    }
}
